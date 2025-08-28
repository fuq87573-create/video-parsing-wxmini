//index.js
//获取应用实例
const app = getApp();
const request = require('../../utils/request.js');
Component({
  options: {
    addGlobalClass: true,
  },
  data: {
    videoUrl: '',
    videoTitle: '',
    isShow: false,
    isDownload: false,
    isButton: true,
    topMiniTitle: '',
    topMiniImg: '',
    topMiniAppId:'',
    topMiniPath:'',
    topMiniInfo: false,
    videoIcon: [],
    // 新增图片相关字段
    imageAtlas: [],
    isImageShow: false,
    contentType: 'video', // 'video' 或 'image'
    // 下载进度相关字段
    downloadProgress: 0,
    downloadSpeed: '',
    downloadStatus: '',
    downloadTask: null,
    showProgress: false,
    estimatedTime: ''
  },

  //组件的生命周期
  lifetimes: {
    created: function () {
      var that = this;
        if (!wx.getStorageSync('openId')) {
        //初始化次数及信息
        var urlContent = app.globalData.url + "wx/login"
        request.requestPostApi(urlContent, {}, this, null, function (res) {
          }) 
      } 

      //初始化头部小程序跳转信息
        var urlContent = app.globalData.url + "wx/initConfig"
        request.requestPostApi(urlContent, {}, this,
        function (res) {
          if (res.status == 200) {
            that.setData({
              topMiniImg: res.data.topMiniImg,
              topMiniTitle: res.data.topMiniTitle,
              topMiniPath: res.data.topMiniPath,
              topMiniAppId: res.data.topMiniAppId,
              videoIcon: res.data.videoIcon
              })
          }
        }, function (res) {
          
        })
    },
    attached: function () {
      // 在组件实例进入页面节点树时执行
    },
    detached: function () {
      // 在组件实例被从页面节点树移除时执行
    },
  },
  //组件的函数
  methods: {
    openVideoInfo: function(){
      wx.navigateTo({
        url: 'test?id=1'
      })
    },
    urlInput: function (e) {
      this.setData({
        videoUrl: e.detail.value.trim()
      })
    },
    // 视频地址匹配是否合法
    regUrl: function (t) {
      return /(http|ftp|https):\/\/[\w\-_]+(\.[\w\-_]+)+([\w\-\.,@?^=%&:/~\+#]*[\w\-\@?^=%&/~\+#])?/.test(t)
    },
    findUrlByStr: function (t) {
      return t.match(/(http|ftp|https):\/\/[\w\-_]+(\.[\w\-_]+)+([\w\-\.,@?^=%&:/~\+#]*[\w\-\@?^=%&/~\+#])?/)
    },
    submit: function () {
      this.setData({
        isButton: false
      })
      if (this.regUrl(this.data.videoUrl)) {
        this.parseVideo();
      } else {
        this.showToast('请复制短视频平台分享链接后再来')
        this.setData({
          isButton: true
        })
      }
    },
    // 头部小程序跳转组件
    mtCps:function(){
      wx.navigateToMiniProgram({
        appId: this.data.topMiniAppId,
        path: this.data.topMiniPath
      })
    },
    // 视频解析
    parseVideo: function () {
      var that = this;
      var params = {
        url: this.data.videoUrl
      };
      request.requestPostApi(app.globalData.url + 'video/getVideoInfo', params, this, function (res) {
        if (res.status != 200) {
          that.showToast('解析失败请检查链接正确性,或重试一次')
        } else {
          let videoUrl = res.data.videoSrc
          let imageAtlas = res.data.imageAtlas || []
          
          // 判断是视频还是图片内容
          if (videoUrl && videoUrl.trim() !== '') {
            // 有视频内容
            if (videoUrl.indexOf("https") == -1) {
              videoUrl = videoUrl.replace('http', 'https')
            }
            that.setData({
              isShow: true,
              url: videoUrl,
              shortUrl: videoUrl.substring(0, 15) + "...",
              contentType: 'video',
              isImageShow: false
            })
          } else if (imageAtlas && imageAtlas.length > 0) {
            // 没有视频但有图片集
            that.setData({
              imageAtlas: imageAtlas,
              isImageShow: true,
              contentType: 'image',
              isShow: false
            })
          } else {
            that.showToast('解析失败，未获取到有效内容')
          }
        }
        that.setData({
          isButton: true
        })
      }, function (res) {
        })
    },
    hideModal() {
      this.setData({
        isShow: false,
        isImageShow: false,
        isUrlDownload: false
      })
    },
    saveVideo() {
      let that = this
      that.setData({
        isDownload: true,
        showProgress: true,
        downloadProgress: 0,
        downloadSpeed: '',
        downloadStatus: '准备下载...',
        estimatedTime: ''
      })
      var t = this;
      wx.getSetting({
        success: function (o) {
          o.authSetting['scope.writePhotosAlbum'] ? t.download() : wx.authorize({
            scope: 'scope.writePhotosAlbum',
            success: function () {
              t.download()
            },
            fail: function (o) {
              t.setData({
                isDownload: false,
                isShow: false,
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

    saveImage(e) {
      let that = this
      let imageUrl = e.currentTarget.dataset.url
      that.setData({
        isDownload: true
      })
      wx.downloadFile({
        url: imageUrl,
        success: function (res) {
          wx.saveImageToPhotosAlbum({
            filePath: res.tempFilePath,
            success: function (res) {
              that.showToast('图片保存成功')
            },
            fail: function (res) {
              that.showToast('图片保存失败')
            },
            complete: function (res) {
              that.setData({
                isDownload: false
              })
            }
          })
        },
        fail: function (res) {
          that.showToast('图片下载失败')
          that.setData({
            isDownload: false
          })
        }
      })
    },
    
    saveAllImages() {
      let that = this
      let imageAtlas = that.data.imageAtlas
      if (!imageAtlas || imageAtlas.length === 0) {
        that.showToast('没有图片可保存')
        return
      }
      
      that.setData({
        isDownload: true
      })
      
      let savedCount = 0
      let failedCount = 0
      let totalCount = imageAtlas.length
      
      imageAtlas.forEach((imageUrl, index) => {
        wx.downloadFile({
          url: imageUrl,
          success: function (res) {
            wx.saveImageToPhotosAlbum({
              filePath: res.tempFilePath,
              success: function (res) {
                savedCount++
                if (savedCount + failedCount === totalCount) {
                  that.setData({
                    isDownload: false
                  })
                  if (failedCount === 0) {
                    that.showToast(`全部图片保存成功(${savedCount}张)`)
                  } else {
                    that.showToast(`保存完成：成功${savedCount}张，失败${failedCount}张`)
                  }
                }
              },
              fail: function (res) {
                failedCount++
                if (savedCount + failedCount === totalCount) {
                  that.setData({
                    isDownload: false
                  })
                  if (failedCount === 0) {
                    that.showToast(`全部图片保存成功(${savedCount}张)`)
                  } else {
                    that.showToast(`保存完成：成功${savedCount}张，失败${failedCount}张`)
                  }
                }
              }
            })
          },
          fail: function (res) {
            failedCount++
            if (savedCount + failedCount === totalCount) {
              that.setData({
                isDownload: false
              })
              if (failedCount === 0) {
                that.showToast(`全部图片保存成功(${savedCount}张)`)
              } else {
                that.showToast(`保存完成：成功${savedCount}张，失败${failedCount}张`)
              }
            }
          }
        })
      })
    },

    download: function () {
      var t = this;
      var originalUrl = this.data.url;
      
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
                isShow: false,
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
    prevent: function () {
      wx.setClipboardData({
        data: this.data.url,
      });
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
    
    showToast: function (text) {
      wx.showToast({
        title: text,
        icon: 'none',
        duration: 2000,
        mask: true
      })
    },
    copy : function(){
      wx.getClipboardData({
        success: res => {
          var str = res.data.trim()
          if (this.regUrl(str)) {
            this.setData({
              videoUrl: this.findUrlByStr(str)[0],
              videoTitle: str.substring(0, 20),
              shortVideoUrl: this.findUrlByStr(str)[0].substring(0, 35) + "...",
            })
          }else{
            this.showToast("请复制短视频平台分享链接后再来")
          }
        }
      })
    }
  },
  pageLifetimes: {
    show: function () {   
      // 如果剪切板内有内容则尝试自动填充
      wx.getClipboardData({
        success: res => {
          var str = res.data.trim()
          if (this.regUrl(str)) {
            wx.showModal({
              title: '检测到剪切板有视频地址，是否自动填入？',
              success: res => {
                if (res.confirm) {
                  this.setData({
                    videoUrl: this.findUrlByStr(str)[0],
                    videoTitle: str.substring(0, 20),
                    shortVideoUrl: this.findUrlByStr(str)[0].substring(0, 35) + "...",
                  })
                }
              }
            })
          }
        }
      })
    },
    hide: function () {
      // 页面被隐藏
    },
    resize: function (size) {
      // 页面尺寸变化
    }
  }
})