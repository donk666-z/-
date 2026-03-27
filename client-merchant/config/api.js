const LOCAL_API_HOST = 'http://127.0.0.1:8080'
const PROD_API_HOST = 'https://your-domain.com'

const envHost =
  typeof process !== 'undefined' && process.env && process.env.VUE_APP_API_HOST
    ? process.env.VUE_APP_API_HOST
    : ''

const storageHost = typeof uni !== 'undefined' ? uni.getStorageSync('apiHost') : ''

// MP-Weixin production must use a whitelisted HTTPS domain.
export const API_HOST =
  storageHost || envHost || (process.env.NODE_ENV === 'development' ? LOCAL_API_HOST : PROD_API_HOST)
export const API_BASE_URL = `${API_HOST}/api`
export const UPLOAD_URL = `${API_BASE_URL}/upload`
