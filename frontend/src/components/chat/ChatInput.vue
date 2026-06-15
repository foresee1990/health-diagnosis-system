<template>
  <div class="chat-input">
    <el-input
      v-model="inputText"
      type="textarea"
      :rows="1"
      :autosize="{ minRows: 1, maxRows: 4 }"
      placeholder="输入您的消息..."
      :disabled="disabled"
      class="chat-input__field"
      @keydown.enter.exact.prevent="submit"
    />
    <el-button
      type="primary"
      :icon="Promotion"
      circle
      :disabled="disabled || !inputText.trim()"
      class="chat-input__send"
      @click="submit"
    />
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { Promotion } from '@element-plus/icons-vue'

const props = defineProps({
  disabled: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['send'])
const inputText = ref('')

function submit() {
  const text = inputText.value.trim()
  if (!text || props.disabled) return
  emit('send', text)
  inputText.value = ''
}
</script>

<style scoped>
.chat-input {
  display: flex;
  align-items: flex-end;
  gap: 10px;
  padding: 16px 32px;
  background: #ffffff;
  border-top: 1px solid var(--color-border);
}

.chat-input__field {
  flex: 1;
}

:deep(.chat-input__field .el-textarea__inner) {
  border-radius: 12px;
  padding: 10px 14px;
  resize: none;
  font-family: inherit;
  font-size: 14px;
  line-height: 1.5;
}

.chat-input__send {
  flex-shrink: 0;
  width: 40px;
  height: 40px;
}
</style>
