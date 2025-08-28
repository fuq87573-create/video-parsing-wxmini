
// 执行登录流程
function performLogin() {
  return new Promise((resolve, reject) => {
    const cachedOpenId = wx.getStorageSync('openId');
    
    if (cachedOpenId) {
      resolve(cachedOpenId);
      return;
    }
    
    // 获取新的openId
    getOpenIdAndLogin().then(resolve).catch(reject);
  });
}

// 获取openId并登录
function getOpenIdAndLogin() {
  return new Promise((resolve, reject) => {
    wx.login({
      success: function(loginRes) {
        if (loginRes.code) {
          // 调用后端接口获取openId
          wx.request({
            url: getApp().globalData.url + 'wx/auth',
            method: 'POST',
            data: {
              js_code: loginRes.code
            },
            header: {
              'Content-Type': "application/x-www-form-urlencoded",
            },
            success: function(authRes) {
              if (authRes.data && authRes.data.status === 200) {
                const openId = authRes.data.data;
                wx.setStorageSync('openId', openId);
                
                // 调用登录接口获取用户信息
                loginUser(openId).then(resolve).catch(reject);
              } else {
                reject(new Error(authRes.data?.message || authRes.data?.msg || '获取openId失败'));
              }
            },
            fail: function(error) {
              reject(new Error('网络请求失败'));
            }
          });
        } else {
          reject(new Error('获取登录凭证失败'));
        }
      },
      fail: function(error) {
        reject(new Error('微信登录失败'));
      }
    });
  });
}

// 用户登录
function loginUser(openId) {
  return new Promise((resolve, reject) => {
    wx.request({
      url: getApp().globalData.url + 'wx/login',
      method: 'POST',
      data: {
        openId: openId
      },
      header: {
        'Content-Type': "application/x-www-form-urlencoded",
      },
      success: function(loginRes) {
        if (loginRes.data && loginRes.data.status === 200) {
          // 保存用户信息
          const userInfo = loginRes.data.data;
          wx.setStorageSync('userInfo', userInfo);
          resolve(openId);
        } else {
          reject(new Error(loginRes.data?.message || loginRes.data?.msg || '登录失败'));
        }
      },
      fail: function(error) {
        reject(new Error('登录请求失败'));
      }
    });
  });
}

function requestPostApi(url, params, sourceObj, successFun, failFun) {
    requestApi(url, params, 'POST', sourceObj, successFun, failFun);
}

function requestGetApi(url, params, sourceObj, successFun, failFun) {
  requestApi(url, params, 'GET', sourceObj, successFun, failFun);
}

function requestDeleteApi(url, params, sourceObj, successFun, failFun) {
  requestApi(url, params, 'DELETE', sourceObj, successFun, failFun);
}

function requestApi(url, params, method, sourceObj, successFun, failFun) {
    // 检查是否需要认证的接口
    const needAuth = !isPublicApi(url);
    
    if (needAuth) {
        // 需要认证的接口
        handleAuthenticatedRequest(url, params, method, sourceObj, successFun, failFun);
    } else {
        // 公开接口，直接请求
        makeRequest(url, params, method, sourceObj, successFun, failFun);
    }
}

// 判断是否为公开接口
function isPublicApi(url) {
    const publicApis = [
        '/wx/auth',
        '/wx/initConfig',
        '/video/health',
        '/video/getSupportedPlatforms'
    ];
    
    return publicApis.some(api => url.includes(api));
}

// 处理需要认证的请求
function handleAuthenticatedRequest(url, params, method, sourceObj, successFun, failFun) {
    const openId = wx.getStorageSync('openId');
    
    if (!openId) {
        // 没有openId，需要重新登录
        performLogin().then((newOpenId) => {
            // 将openId添加到请求参数中
            const authParams = { ...params, openId: newOpenId };
            makeRequest(url, authParams, method, sourceObj, successFun, failFun);
        }).catch(error => {
            
            typeof failFun == 'function' && failFun({ msg: '登录失败，请重试' }, sourceObj);
        });
        return;
    }
    
    // 将openId添加到请求参数中
    const authParams = { ...params, openId: openId };
    makeRequest(url, authParams, method, sourceObj, successFun, failFun);
}

// 发起HTTP请求
function makeRequest(url, params, method, sourceObj, successFun, failFun) {
    const headers = {
        'Content-Type': "application/x-www-form-urlencoded"
    };
    
    // 打印请求信息用于调试

    wx.request({
        url: url,
        method: method,
        data: params,
        header: headers,
        success: function (res) {

            typeof successFun == 'function' && successFun(res.data, sourceObj);
        },
        fail: function (res) {

            typeof failFun == 'function' && failFun(res.data, sourceObj);
        },
        complete: function(res) {
            wx.hideLoading();
        }
    });
}

module.exports = {
    requestPostApi,
    requestGetApi,
    requestDeleteApi
}