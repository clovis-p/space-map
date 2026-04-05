<template>
  <div class="app">
    <SolarSystemView
      ref="solarSystemView"
      @bodies-loaded="bodies = $event"
    />
    <div class="hud-top-container">
      <div class="hud-top">
        <TimeControls />
      </div>
    </div>
    <div class="hud-side" :class="{ 'hud-side--hidden': !hudVisible }">
      <BodyList
        :bodies="bodies"
        :focused-id="focusedId"
        :groups="groups"
        @focus="onFocus"
        @spacecraft-loaded="onSpacecraftLoaded"
        @focus-spacecraft="onFocusSpacecraft"
        @spacecraft-cleared="onSpacecraftCleared"
      />
    </div>
    <button class="hud-toggle" @click="hudVisible = !hudVisible" :aria-label="hudVisible ? 'Hide panel' : 'Show panel'">
      <svg v-if="hudVisible" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor">
        <path stroke-linecap="round" stroke-linejoin="round" d="M6 18 18 6M6 6l12 12" />
      </svg>
      <svg v-else xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor">
        <path stroke-linecap="round" stroke-linejoin="round" d="M3.75 6.75h16.5M3.75 12h16.5m-16.5 5.25h16.5" />
      </svg>
    </button>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import SolarSystemView from './features/solar-system/view/SolarSystemView.vue';
import TimeControls from './features/ui/TimeControls.vue';
import BodyList from './features/ui/BodyList.vue';
import { fetchGroups } from './services/api.js';

const solarSystemView = ref(null);
const bodies = ref([]);
const focusedId = ref(null);
const groups = ref([]);
const hudVisible = ref(true);

onMounted(async () => {
  groups.value = await fetchGroups();
});

function onFocus(id) {
  focusedId.value = id;
  solarSystemView.value?.focusBody(id);
}

function onSpacecraftLoaded(data) {
  solarSystemView.value?.loadSpacecraft(data);
}

function onFocusSpacecraft(id) {
  solarSystemView.value?.focusSpacecraft(id);
}

function onSpacecraftCleared() {
  solarSystemView.value?.clearSpacecraft();
}
</script>

<style scoped>
.app {
  position: relative;
  width: 100%;
  height: 100%;
}

.hud-top-container {
  display: flex;
  position: absolute;
  width: 100svw;
  top: 16px;
}

.hud-top {
  margin: auto;
  display: flex;
}

.hud-side {
  position: absolute;
  top: 16px;
  left: 16px;
}

.hud-toggle {
  display: none;
}

/* Mobile: body list moves to the bottom above the clock */
@media (max-width: 640px) {
  .hud-side {
    top: auto;
    left: 0;
    right: 0;
    bottom: 0;
    padding: 12px;
    transition: transform 0.2s ease, opacity 0.2s ease;
  }

  .hud-side--hidden {
    transform: translateY(100%);
    opacity: 0;
    pointer-events: none;
  }

  .hud-toggle {
    display: flex;
    align-items: center;
    justify-content: center;
    position: absolute;
    bottom: 16px;
    right: 16px;
    width: 40px;
    height: 40px;
    border-radius: 8px;
    border: 1px solid rgba(255, 255, 255, 0.1);
    background: rgba(0, 0, 0, 0.6);
    color: rgba(255, 255, 255, 0.8);
    cursor: pointer;
    backdrop-filter: blur(8px);
  }

  .hud-toggle svg {
    width: 20px;
    height: 20px;
  }
}
</style>
