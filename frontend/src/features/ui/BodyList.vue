<template>
  <nav class="body-list">
    <button
      v-for="body in bodies"
      :key="body.id"
      class="body-item"
      :class="{ active: focusedId === body.id }"
      @click="emit('focus', body.id)"
    >
      <span class="dot" :style="{ background: colorToCss(body.color) }" />
      <span class="name">{{ body.name }}</span>
    </button>
  </nav>
</template>

<script setup>
const props = defineProps({
  bodies: { type: Array, default: () => [] },
  focusedId: { type: String, default: null },
});

const emit = defineEmits(['focus']);

function colorToCss(hex) {
  return `#${hex.toString(16).padStart(6, '0')}`;
}
</script>

<style scoped>
.body-list {
  display: flex;
  flex-direction: column;
  gap: 2px;
  background: rgba(0, 0, 0, 0.6);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 8px;
  padding: 6px;
  backdrop-filter: blur(8px);
}

.body-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 10px;
  border-radius: 5px;
  border: none;
  background: transparent;
  color: rgba(255, 255, 255, 0.7);
  font-size: 13px;
  cursor: pointer;
  text-align: left;
  white-space: nowrap;
  transition: background 0.15s, color 0.15s;
}

.body-item:hover {
  background: rgba(255, 255, 255, 0.08);
  color: #fff;
}

.body-item.active {
  background: rgba(255, 255, 255, 0.12);
  color: #fff;
}

.dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  flex-shrink: 0;
}

/* Mobile: horizontal strip at the bottom */
@media (max-width: 640px) {
  .body-list {
    flex-direction: row;
    overflow-x: auto;
    scrollbar-width: none;
    border-radius: 8px;
    padding: 4px;
  }

  .body-list::-webkit-scrollbar {
    display: none;
  }

  .body-item {
    flex-shrink: 0;
    flex-direction: column;
    gap: 4px;
    padding: 6px 8px;
    font-size: 11px;
    align-items: center;
  }
}
</style>
