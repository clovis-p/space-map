<template>
  <div class="solar-system-view">
    <canvas ref="canvasRef" />
    <div class="labels">
      <div
        v-for="label in labels"
        :key="label.id"
        class="label"
        :style="{ left: label.x + 'px', top: label.y + 'px' }"
      >
        {{ label.name }}
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, shallowRef, onMounted, onUnmounted } from 'vue';
import { SolarSystemController } from '../controllers/SolarSystemController.js';

const emit = defineEmits(['bodies-loaded']);

const canvasRef = ref(null);
// shallowRef: Vue won't attempt deep reactivity on the controller
const controller = shallowRef(null);

const labels = ref([]);

let bodies = null;

function updateLabels() {
  if (!controller.value || !bodies){
    return;
  }

  const canvas = canvasRef.value;
  const scene = controller.value._sceneRenderer.scene;
  const camera = controller.value._sceneRenderer.camera;
  const width = canvas.clientWidth;
  const height = canvas.clientHeight;

  const result = [];
  for (const [id, body] of bodies) {
    let mesh = null;
    scene.traverse((child) => {
      if (child.isMesh && child.userData.bodyId === id) {mesh = child;}
    });
    if (!mesh) {continue;}

    // Project 3D position to 2D screen
    const pos = mesh.position.clone();
    pos.project(camera);
    const x = (pos.x * 0.5 + 0.5) * width;
    const y = (-pos.y * 0.5 + 0.5) * height;

    // Only show label if in front of camera
    if (pos.z < 1) {
      result.push({ id, name: body.name, x, y });
    }
  }
  labels.value = result;
}

onMounted(() => {
  controller.value = new SolarSystemController(canvasRef.value, {
    onBodiesLoaded(loadedBodies) {
      bodies = loadedBodies;
      emit('bodies-loaded', [...loadedBodies.values()].map(b => ({ id: b.id, name: b.name, color: b.color })));
    },
    onAfterRender() {
      updateLabels();
    },
  });
});

onUnmounted(() => {
  controller.value?.dispose();
});

defineExpose({
  focusBody: (id) => controller.value?.focusBody(id),
});
</script>

<style scoped>
.solar-system-view {
  position: relative;
  width: 100%;
  height: 100%;
}

canvas {
  display: block;
  width: 100%;
  height: 100%;
}

.labels {
  position: absolute;
  inset: 0;
  pointer-events: none;
}

.label {
  position: absolute;
  transform: translate(-50%, -150%);
  font-size: 11px;
  color: rgba(255, 255, 255, 0.7);
  white-space: nowrap;
  text-shadow: 0 0 4px #000;
  pointer-events: none;
}
</style>
