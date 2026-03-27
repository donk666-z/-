export const getToken = () => uni.getStorageSync('token') || ''

export const getStoredUserInfo = () => {
  const userInfo = uni.getStorageSync('userInfo')
  return userInfo && typeof userInfo === 'object' ? userInfo : null
}

export const isRiderToken = () => {
  const token = getToken()
  const userInfo = getStoredUserInfo()
  return !!token && !!userInfo && userInfo.role === 'rider'
}

export const hasLogin = () => isRiderToken()

export const clearSession = () => {
  uni.removeStorageSync('token')
  uni.removeStorageSync('userInfo')
  uni.removeStorageSync('riderStatus')
}

export const goLogin = () => {
  const pages = typeof getCurrentPages === 'function' ? getCurrentPages() : []
  const current = pages.length ? pages[pages.length - 1].route : ''
  if (current === 'pages/login/index') {
    return
  }
  uni.reLaunch({ url: '/pages/login/index' })
}

export const ensureLogin = () => {
  if (hasLogin()) {
    return true
  }
  clearSession()
  goLogin()
  return false
}

export const handleUnauthorized = () => {
  clearSession()
  goLogin()
}
