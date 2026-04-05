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
    <div class="hud-side">
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

/* Mobile: body list moves to the bottom above the clock */
@media (max-width: 640px) {
  .hud-side {
    top: auto;
    left: 0;
    right: 0;
    bottom: 0;
    padding: 12px;
  }
}
</style>
