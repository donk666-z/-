export const clearSession = () => {
  uni.removeStorageSync('token')
  uni.removeStorageSync('userInfo')
}

export const goLogin = () => {
  const pages = typeof getCurrentPages === 'function' ? getCurrentPages() : []
  const current = pages.length ? pages[pages.length - 1].route : ''
  if (current === 'pages/login/index') {
    return
  }
  uni.reLaunch({ url: '/pages/login/index' })
}

export const handleUnauthorized = () => {
  clearSession()
  goLogin()
}

