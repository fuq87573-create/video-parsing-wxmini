const app = getApp();
const request = require('../../utils/request.js');
Page({
  /**
   * 页面的初始数据
   */
  data: {
    parsingInfo : null,
    isDownload : false,
    downloadText: '保存中...',
    // 下载进度相关字段
    downloadProgress: 0,
    downloadSpeed: '',
    downloadStatus: '',
    downloadTask: null,
    showProgress: false,
    estimatedTime: '',
    currentDownloadUrl: ''
  },
  /**
   * 生命周期函数--监听页面加载
   */
  onLoad: function (options) {
    var thar = this;
    var urlContent = app.globalData.url + "video/getParsingInfo"
    // 获取openId参数
    let openId = wx.getStorageSync('openId');
    if (!openId) {
      thar.showToast('请先登录');
      return;
    }
    
    var params = {
      openId: openId
    };
    
    request.requestGetApi(urlContent, params, this, function (res) {
      if (res.status == 200) {
        if(res.data && res.data.records && res.data.records.length > 0){
          // 处理图片集数据和日期格式化
          const processedRecords = res.data.records.map(record => {
            if (record.imageAtlas) {
              try {
                record.imageAtlas = JSON.parse(record.imageAtlas);
              } catch (e) {
                
                record.imageAtlas = [];
              }
            }
            // 格式化日期
            if (record.createTime) {
              record.formatTime = thar.formatDateTime(record.createTime);
            }
            return record;
          });
          
          thar.setData({
            parsingInfo: processedRecords
          })
        }
      } else {
        }
    }, function (res) {
      });
  },

  // 格式化日期时间
  formatDateTime: function(dateTimeStr) {
    if (!dateTimeStr) return '未知时间';
    
    try {
      const date = new Date(dateTimeStr);
      const year = date.getFullYear();
      const month = String(date.getMonth() + 1).padStart(2, '0');
      const day = String(date.getDate()).padStart(2, '0');
      const hours = String(date.getHours()).padStart(2, '0');
      const minutes = String(date.getMinutes()).padStart(2, '0');
      
      return `${year}-${month}-${day} ${hours}:${minutes}`;
    } catch (e) {
      
      return '时间格式错误';
    }
  },
   
  saveImages: function(res) {
    let that = this;
    that.setData({
      isDownload: true,
      downloadText: '图片保存中...'
    })
    
    const images = res.currentTarget.dataset.images;
    if (!images || images.length === 0) {
      that.showToast('没有图片可下载');
      that.setData({ isDownload: false });
      return;
    }
    
    wx.getSetting({
      success: function (o) {
        if (o.authSetting['scope.writePhotosAlbum']) {
          that.downloadImages(images);
        } else {
          wx.authorize({
            scope: 'scope.writePhotosAlbum',
            success: function () {
              that.downloadImages(images);
            },
            fail: function (o) {
              that.showToast('需要授权保存到相册');
              that.setData({ isDownload: false });
            }
          })
        }
      }
    })
  },
  
  downloadImages: function(images) {
    let that = this;
    let successCount = 0;
    let totalCount = images.length;
    
    images.forEach((imageUrl, index) => {
      wx.downloadFile({
        url: imageUrl,
        success: function(res) {
          if (res.statusCode === 200) {
            wx.saveImageToPhotosAlbum({
              filePath: res.tempFilePath,
              success: function() {
                successCount++;
                if (successCount === totalCount) {
                  that.showToast(`成功保存${successCount}张图片`);
                  that.setData({ isDownload: false });
                }
              },
              fail: function() {
                
                successCount++; // 仍然计数，避免卡住
                if (successCount === totalCount) {
                  that.showToast(`保存完成，部分图片可能失败`);
                  that.setData({ isDownload: false });
                }
              }
            })
          } else {
            
            successCount++;
            if (successCount === totalCount) {
              that.showToast(`保存完成，部分图片可能失败`);
              that.setData({ isDownload: false });
            }
          }
        },
        fail: function() {
          
          successCount++;
          if (successCount === totalCount) {
            that.showToast(`保存完成，部分图片可能失败`);
            that.setData({ isDownload: false });
          }
        }
      })
    });
  },

  saveVideo:function(res){
    let that = this
    that.setData({
      isDownload: true,
      downloadText: '视频保存中...',
      showProgress: true,
      downloadProgress: 0,
      downloadSpeed: '',
      downloadStatus: '准备下载...',
      estimatedTime: ''
    })
    var t = this;
    var url = res.currentTarget.dataset.url;
    if (url.indexOf("https") == -1) {
      url = url.replace('http', 'https')
    }
    
    // 保存当前下载的URL
    t.setData({
      currentDownloadUrl: url
    });
    
    wx.getSetting({
      success: function (o) {
        o.authSetting['scope.writePhotosAlbum'] ? t.downloadWithProgress(url) : wx.authorize({
          scope: 'scope.writePhotosAlbum',
          success: function () {
            t.downloadWithProgress(url)
          },
          fail: function (o) {
            t.setData({
              isDownload: false
            })
            wx.showModal({
              title: '提示',
              content: '视频保存到相册需获取相册权限请允许开启权限',
              confirmText: '确认',
              cancelText: '取消',
              success: function (o) {
                o.confirm ? (wx.openSetting({
                  success: function (o) {}
                })) : ''
              }
            })
          }
        })
      }
    })
  },
  // 带进度的下载方法
  downloadWithProgress: function(originalUrl) {
    var t = this;
    
    // 显示下载进度
    t.setData({
      showProgress: true,
      downloadProgress: 0,
      downloadStatus: '开始下载...'
    });
    
    // 直接使用微信接口下载原始URL
    const downloadTask = wx.downloadFile({
      url: originalUrl,
      success: function (o) {
        t.setData({
          downloadStatus: '下载完成，正在保存...',
          downloadTask: null
        });
        
        wx.saveVideoToPhotosAlbum({
          filePath: o.tempFilePath,
          success: function (o) {
            t.showToast('视频保存成功')
            t.setData({
              isDownload: false,
              showProgress: false,
              downloadProgress: 0
            })
          },
          fail: function (o) {
            
            t.showToast('视频保存失败，请检查相册权限')
            t.setData({
              isDownload: false,
              showProgress: false,
              downloadProgress: 0
            })
          }
        })
      },
      fail: function (o) {
        
        t.showToast('视频下载失败')
        t.setData({
          isDownload: false,
          showProgress: false,
          downloadProgress: 0,
          downloadTask: null
        })
      }
    });
    
    // 保存下载任务引用
    t.setData({
      downloadTask: downloadTask
    });
    
    // 监听下载进度
    downloadTask.onProgressUpdate((res) => {
      const progress = Math.round(res.progress);
      const downloadedSize = (res.totalBytesWritten / 1024 / 1024).toFixed(2);
      const totalSize = (res.totalBytesExpectedToWrite / 1024 / 1024).toFixed(2);
      
      t.setData({
        downloadProgress: progress,
        downloadStatus: `下载中... ${progress}% (${downloadedSize}MB/${totalSize}MB)`
      });
    });
  },
  
  // 原有的简单下载方法（保留作为备用）
  download: function (res) {
    var t = this,
    e = res;
    wx.downloadFile({
      url: e,
      success: function (o) {
        wx.saveVideoToPhotosAlbum({
          filePath: o.tempFilePath,
          success: function (o) {
            t.showToast('视频保存成功')
            t.setData({
              isDownload: false
            })
          },
          fail: function (o) {
            t.showToast('视频保存失败')
            t.setData({
              isDownload: false
            })
          }
        })
      },
      fail: function (o) {
        t.showToast('视频保存失败')
        t.setData({
          isDownload: false,
        })
      }
    })
  },
  // 取消下载
  cancelDownload: function() {
    let that = this;
    if (that.data.downloadTask) {
      that.data.downloadTask.abort();
      that.setData({
        downloadTask: null,
        isDownload: false,
        showProgress: false,
        downloadProgress: 0,
        downloadStatus: ''
      });
      that.showToast('下载已取消');
    }
  },
  
  // 格式化时间
  formatDateTime: function(dateTimeStr) {
    if (!dateTimeStr) return '';
    
    const date = new Date(dateTimeStr);
    const now = new Date();
    const diff = now.getTime() - date.getTime();
    const minutes = Math.floor(diff / (1000 * 60));
    
    if (minutes < 1) {
      return '刚刚';
    } else if (minutes < 60) {
      return `${minutes}分钟前`;
    } else {
      const month = (date.getMonth() + 1).toString().padStart(2, '0');
      const day = date.getDate().toString().padStart(2, '0');
      const hours = date.getHours().toString().padStart(2, '0');
      const mins = date.getMinutes().toString().padStart(2, '0');
      return `${month}-${day} ${hours}:${mins}`;
    }
  },
  
  showToast: function (text) {
    wx.showToast({
      title: text,
      icon: 'none',
      duration: 2000,
      mask: true
    })
  },
  /**
   * 生命周期函数--监听页面初次渲染完成
   */
  onReady: function () {

  },

  /**
   * 生命周期函数--监听页面显示
   */
  onShow: function () {

  },

  /**
   * 生命周期函数--监听页面隐藏
   */
  onHide: function () {

  },

  /**
   * 生命周期函数--监听页面卸载
   */
  onUnload: function () {

  },

  /**
   * 页面相关事件处理函数--监听用户下拉动作
   */
  onPullDownRefresh: function () {

  },

  /**
   * 页面上拉触底事件的处理函数
   */
  onReachBottom: function () {

  },

  /**
   * 用户点击右上角分享
   */
  onShareAppMessage: function () {

  }
});
