const img = document.getElementById("frame") as HTMLImageElement;
const stats = document.getElementById("stats") as HTMLDivElement;

let fps = 0;
let frameCount = 0;
let lastTime = Date.now();

const ws = new WebSocket("ws://192.168.89.216:8080");
ws.onmessage = (event) => {
    img.src = event.data;  // update frame
    frameCount++;
};

setInterval(() => {
    const now = Date.now();
    fps = Math.round(frameCount * 1000 / (now - lastTime));
    stats.innerText = `FPS: ${fps} | Resolution: 640x480`;
    frameCount = 0;
    lastTime = now;
}, 1000);
