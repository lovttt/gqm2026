import { defineStore } from 'pinia'

export const useAuth = defineStore('auth', {
  state: () => ({
    token: localStorage.getItem('token') || '',
    username: localStorage.getItem('username') || '',
    role: localStorage.getItem('role') || '',
    studentId: localStorage.getItem('studentId') || ''
  }),
  actions: {
    setAuth(data) {
      this.token = data.token
      this.username = data.username
      this.role = data.role
      this.studentId = data.studentId || ''
      localStorage.setItem('token', data.token)
      localStorage.setItem('username', data.username)
      localStorage.setItem('role', data.role)
      localStorage.setItem('studentId', this.studentId)
    },
    logout() {
      this.token = ''
      this.username = ''
      this.role = ''
      this.studentId = ''
      localStorage.clear()
    }
  }
})
