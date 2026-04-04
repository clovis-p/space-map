<template>
  <nav class="body-list">
    <div class="tabs">
      <button class="tab-btn" :class="{ active: activeTab === 'planets' }" @click="activeTab = 'planets'">Planets</button>
      <button class="tab-btn" :class="{ active: activeTab === 'satellites' }" @click="onSatellitesTab">Satellites</button>
    </div>

    <template v-if="activeTab === 'planets'">
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
    </template>

    <template v-else>
      <select class="group-select" v-model="selectedGroupId" @change="fetchSatellites">
        <option v-for="g in groups" :key="g.id" :value="g.id">{{ g.name }}</option>
      </select>
      <div v-if="loadingSatellites" class="loading">Loading…</div>
      <button
        v-else
        v-for="sat in satellites"
        :key="sat.id"
        class="body-item"
      >
        <span class="dot" :style="{ background: colorToCss(selectedGroup?.color) }" />
        <span class="name">{{ sat.name }}</span>
      </button>
    </template>
  </nav>
</template>

<script setup>
import { ref, computed } from 'vue';

const props = defineProps({
  bodies: { type: Array, default: () => [] },
  focusedId: { type: String, default: null },
  groups: { type: Array, default: () => [] },
});

const emit = defineEmits(['focus']);

const activeTab = ref('planets');
const selectedGroupId = ref('stations');
const satellites = ref([]);
const loadingSatellites = ref(false);

const selectedGroup = computed(() => props.groups.find(g => g.id === selectedGroupId.value));

function colorToCss(hex) {
  if (hex == null) return '#888';
  return `#${hex.toString(16).padStart(6, '0')}`;
}

async function fetchSatellites() {
  loadingSatellites.value = true;
  const res = await fetch(`/api/satellites?group=${selectedGroupId.value}`);
  satellites.value = await res.json();
  loadingSatellites.value = false;
}

function onSatellitesTab() {
  activeTab.value = 'satellites';
  if (satellites.value.length === 0) fetchSatellites();
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
  max-height: 80vh;
  overflow-y: auto;
  scrollbar-width: thin;
  scrollbar-color: rgba(255,255,255,0.15) transparent;
}

.tabs {
  display: flex;
  gap: 2px;
  margin-bottom: 4px;
}

.tab-btn {
  flex: 1;
  padding: 5px 8px;
  border-radius: 5px;
  border: none;
  background: transparent;
  color: rgba(255, 255, 255, 0.5);
  font-size: 12px;
  cursor: pointer;
  transition: background 0.15s, color 0.15s;
}

.tab-btn:hover {
  background: rgba(255, 255, 255, 0.08);
  color: #fff;
}

.tab-btn.active {
  background: rgba(255, 255, 255, 0.12);
  color: #fff;
}

.group-select {
  width: 100%;
  padding: 5px 8px;
  border-radius: 5px;
  border: 1px solid rgba(255, 255, 255, 0.15);
  background: rgba(255, 255, 255, 0.06);
  color: rgba(255, 255, 255, 0.8);
  font-size: 12px;
  cursor: pointer;
  margin-bottom: 2px;
  outline: none;
}

.group-select:focus {
  border-color: rgba(255, 255, 255, 0.3);
}

.loading {
  padding: 8px 10px;
  color: rgba(255, 255, 255, 0.4);
  font-size: 12px;
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

@media (max-width: 640px) {
  .body-list {
    flex-direction: column;
    max-height: 40vh;
    border-radius: 8px;
    padding: 4px;
  }
}
</style>
