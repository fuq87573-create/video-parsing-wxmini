const request = require('../../utils/request.js');
const app = getApp();
Component({
  options: {
    addGlobalClass: true,
  },
  data: {
    starCount: 0,
    forksCount: 0,
    visitTotal: 0,
    success: false,
    error: false,
    continuousSignDays: 0,
    rewardPoints: 10,
    errorMsg: '',
    todaySignin: false,  // 默认未签到状态
    avatarUrl: '',
    nickName: '',
    nickNameSet: false,  // 标记昵称是否已设置
    loading: false  // 加载状态
  },
  attached() {
    let that = this;
    
    // 获取用户信息
    const userInfo = wx.getStorageSync('userInfo');
    if (userInfo && userInfo.avatarUrl && userInfo.nickName) {
      that.setData({
        avatarUrl: userInfo.avatarUrl,
        nickName: userInfo.nickName,
        nickNameSet: userInfo.nickName && userInfo.nickName !== '微信用户'
      });
    }
    
    numDH();
    function numDH() {
      var urlContent = app.globalData.url + "wx/login"
      request.requestPostApi(urlContent, {}, this, function (res) {
        
        if (res.status == 200) {

          
          that.setData({
            starCount: that.coutNum(res.data.signInSum || 0),
            visitTotal: that.coutNum(res.data.points || 0),
            continuousSignDays: res.data.continuousSignDays || 0,
            todaySignin: res.data.todaySignin || false
          })
          
        } else {
          }
      }, function (res) {
        });
    }
  },
  methods: {
    coutNum(e) {
      // 确保输入是数字
      if (typeof e !== 'number') {
        e = parseInt(e) || 0;
      }
      
      if (e >= 10000) {
        return (e / 10000).toFixed(1) + 'W';
      }
      if (e >= 1000) {
        return (e / 1000).toFixed(1) + 'K';
      }
      return e.toString();
    },
    CopyLink(e) {
      wx.setClipboardData({
        data: e.currentTarget.dataset.link,
        success: res => {
          wx.showToast({
            title: '已复制',
            duration: 1000,
          })
        }
      })
    },
    showModal(e) {
      this.setData({
        modalName: e.currentTarget.dataset.target
      })
    },
    hideModal(e) {
      this.setData({
        modalName: null
      })
    },
    showQrcode() {
      // 显示加载框
      this.setData({
        loading: true
      });
      
      // 通过后端API获取作者微信二维码
      const app = getApp();
      const imageUrl = app.globalData.url + 'static/images/yourQrcode.jpg';
      
      // 先测试图片是否可以访问
      wx.downloadFile({
        url: imageUrl,
        success: (res) => {
          // 隐藏加载框
          this.setData({
            loading: false
          });
          
          if (res.statusCode === 200) {
            wx.previewImage({
              urls: [imageUrl],
              current: imageUrl
            })
          } else {
            wx.showModal({
              title: '图片加载失败',
              content: `服务器返回${res.statusCode}错误，图片文件不存在。\n请求URL: ${imageUrl}\n请确认后端服务器 static/images/yourQrcode.jpg 文件是否存在。`,
              showCancel: false,
              confirmText: '知道了'
            })
          }
        },
        fail: (err) => {
          // 隐藏加载框
          this.setData({
            loading: false
          });
          
          wx.showModal({
            title: '网络错误',
            content: `请求失败: ${err.errMsg}\n请求URL: ${imageUrl}\n请检查网络连接和服务器状态`,
            showCancel: false,
            confirmText: '知道了'
          })
        }
      })
    },
    sign() {
      let that = this;
      
      // 检查是否已经签到
      if (that.data.todaySignin) {
        that.showToast('今日已签到，请明天再来');
        return;
      }
      
      var urlContent = app.globalData.url + "wx/signIn"
      // 获取openId参数
      let openId = wx.getStorageSync('openId');
      if (!openId) {
        that.showToast('请先登录');
        return;
      }
      
      var params = {
        openId: openId
      };
      
      request.requestPostApi(urlContent, params, this, function (res) {
        
        if (res.status == 200) {
          // 使用后端返回的最新数据更新页面
          that.setData({
            starCount: that.coutNum(res.data.signInSum || 0),
            visitTotal: that.coutNum(res.data.points || 0),
            success: true,
            continuousSignDays: res.data.continuousSignDays || 0,
            rewardPoints: res.data.rewardPoints || 10,
            todaySignin: true  // 签到成功后设置为已签到状态
          })
          
        } else if (res.status == 500 && res.message && res.message.includes('已签到')) {
          // 处理今日已签到的情况
          that.setData({
            todaySignin: true,  // 更新签到状态
            error: true,
            errorMsg: res.message || '今日已签到'
          })
          
        } else {
          that.setData({
            error: true,
            errorMsg: res.message || '签到失败，请稍后重试'
          })
        }
      }, function (res) {
        that.setData({
          error: true,
          errorMsg: '网络异常，请稍后重试'
        })
      });
    },
    hideModal(){
      this.setData({
        error: false,
        success: false
      })
    },
    

    
    showToast: function(text) {
      wx.showToast({
        title: text,
        icon: 'none',
        duration: 2000,
        mask: true
      })
    },
    
    // 选择头像
    onChooseAvatar: function(e) {
      const { avatarUrl } = e.detail;
      this.setData({
        avatarUrl: avatarUrl
      });
      this.saveAvatarToServer(avatarUrl);
    },
    
    // 昵称输入
    bindNicknameInput: function(e) {
      // 如果昵称已设置，则不允许修改
      if (this.data.nickNameSet) {
        return;
      }
      this.setData({
        nickName: e.detail.value
      });
    },
    
    // 昵称输入完成
    bindNicknameBlur: function(e) {
      // 如果昵称已设置，则不允许修改
      if (this.data.nickNameSet) {
        return;
      }
      const nickName = e.detail.value.trim();
      if (nickName) {
        this.saveNicknameToServer(nickName);
      }
    },
    
    // 保存头像到服务器
    saveAvatarToServer: function(avatarUrl) {
      const openId = wx.getStorageSync('openId');
      if (!openId) {
        this.showToast('请先登录');
        return;
      }
      
      // 获取当前昵称，如果没有则使用默认值
      const currentNickName = this.data.nickName || '微信用户';
      
      const params = {
        openId: openId,
        avatarUrl: avatarUrl,
        nickName: currentNickName
      };
      
      request.requestPostApi(app.globalData.url + 'wx/updateUserInfo', params, this, 
        function(res) {
          if (res.status === 200) {
            // 更新本地缓存
            const userInfo = wx.getStorageSync('userInfo') || {};
            userInfo.avatarUrl = avatarUrl;
            wx.setStorageSync('userInfo', userInfo);
          }
        },
        function(res) {
        }
      );
    },
    
    // 保存昵称到服务器
    saveNicknameToServer: function(nickName) {
      const openId = wx.getStorageSync('openId');
      if (!openId) {
        this.showToast('请先登录');
        return;
      }
      
      // 获取当前头像，如果没有则使用默认值
      const currentAvatarUrl = this.data.avatarUrl || '/images/default-avatar.png';
      
      const params = {
        openId: openId,
        nickName: nickName,
        avatarUrl: currentAvatarUrl
      };
      
      request.requestPostApi(app.globalData.url + 'wx/updateUserInfo', params, this,
        function(res) {
          if (res.status === 200) {
            // 更新本地缓存
            const userInfo = wx.getStorageSync('userInfo') || {};
            userInfo.nickName = nickName;
            wx.setStorageSync('userInfo', userInfo);
            // 标记昵称已设置
            this.setData({
              nickNameSet: true
            });
          }
        },
        function(res) {
        }
      );
    }
  }
})