<template>
  <div class="time-controls">
    <span class="time-label">{{ formattedTime }}</span>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue';

/**
 * For MVP: displays the current real-time UTC clock.
 *
 * When time scrubbing is implemented, wire this component via:
 *   - prop: currentTime (number, ms) : override the displayed time
 *   - emit: timeChange (number, ms) : user has scrubbed to a new time
 */

const now = ref(Date.now());
let intervalId = null;

onMounted(() => {
  intervalId = setInterval(() => { now.value = Date.now(); }, 1000);
});

onUnmounted(() => {
  clearInterval(intervalId);
});

const formattedTime = computed(() => {
  return new Date(now.value).toUTCString();
});
</script>

<style scoped>
.time-controls {
  background: rgba(0, 0, 0, 0.6);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 6px;
  padding: 6px 12px;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.8);
  font-variant-numeric: tabular-nums;
}
</style>
