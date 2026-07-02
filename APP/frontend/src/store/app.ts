import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useAppStore = defineStore('app', () => {
  const sidebarCollapsed = ref(false)
  const currentPage = ref('')

  function toggleSidebar() {
    sidebarCollapsed.value = !sidebarCollapsed.value
  }

  function setCurrentPage(page: string) {
    currentPage.value = page
  }

  return { sidebarCollapsed, currentPage, toggleSidebar, setCurrentPage }
}, { persist: true })