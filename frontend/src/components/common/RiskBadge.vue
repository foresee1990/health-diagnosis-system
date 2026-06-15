<template>
  <el-tag
    v-if="level"
    :type="tagType"
    size="small"
    round
    class="risk-badge"
  >
    {{ label }}
  </el-tag>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  level: {
    type: String,
    default: null
  }
})

const tagType = computed(() => {
  const map = {
    low: 'success',
    medium: 'warning',
    high: 'danger',
    urgent: 'danger'
  }
  return map[props.level] || 'info'
})

const label = computed(() => {
  const map = {
    low: '低风险',
    medium: '中等风险',
    high: '高风险',
    urgent: '紧急'
  }
  return map[props.level] || props.level
})
</script>

<style scoped>
.risk-badge {
  font-weight: 500;
  letter-spacing: 0.02em;
}
</style>
