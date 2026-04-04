<template>
  <div class="app">
    <SolarSystemView
      ref="solarSystemView"
      @bodies-loaded="bodies = $event"
    />
    <div class="hud-bottom">
      <TimeControls />
    </div>
    <div class="hud-side">
      <BodyList
        :bodies="bodies"
        :focused-id="focusedId"
        :groups="groups"
        @focus="onFocus"
        @satellites-loaded="onSatellitesLoaded"
        @focus-satellite="onFocusSatellite"
        @satellites-cleared="onSatellitesCleared"
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

function onSatellitesLoaded(data) {
  solarSystemView.value?.loadSatellites(data);
}

function onFocusSatellite(id) {
  solarSystemView.value?.focusSatellite(id);
}

function onSatellitesCleared() {
  solarSystemView.value?.clearSatellites();
}
</script>

<style scoped>
.app {
  position: relative;
  width: 100%;
  height: 100%;
}

.hud-bottom {
  position: absolute;
  bottom: 16px;
  left: 50%;
  transform: translateX(-50%);
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
    bottom: 52px;
    padding: 0 12px;
  }
}
</style>
