import { defineStore } from 'pinia'
import { ref, onMounted } from 'vue'
import { login as loginApi, logout as logoutApi } from '@/api/auth'
import type { LoginDTO } from '@/api/auth'

export const useUserStore = defineStore('user', () => {
  const token = ref('')
  const userId = ref(0)
  const username = ref('')

  onMounted(() => {
    token.value = ''
    userId.value = 0
    username.value = ''
  })

  function login(data: LoginDTO) {
    return loginApi(data).then((res) => {
      token.value = res.data.token
      userId.value = res.data.userId
      username.value = res.data.username
    })
  }

  function logout() {
    return logoutApi().then(() => {
      token.value = ''
      userId.value = 0
      username.value = ''
    })
  }

  function clear() {
    token.value = ''
    userId.value = 0
    username.value = ''
  }

  return { token, userId, username, login, logout, clear }
})