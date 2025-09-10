const { parentPort, workerData } = require("worker_threads");
const fs = require("fs");

parentPort.on("message", (data) => {
  const { server, version, author, dimension, chunkX, chunkZ, blocks } = data;

  const chunkPath = `${workerData.worldSavePath}/${server}/${dimension}`;
  if (!fs.existsSync(chunkPath)) fs.mkdirSync(chunkPath, { recursive: true });

  const chunkFilePath = `${chunkPath}/${chunkX} ${chunkZ} ${author} ${version}.bc`; // bevelchunk format frfr
  const blockMap = new Map();

  if (fs.existsSync(chunkFilePath)) {
    const blockArray = fs.readFileSync(chunkFilePath).toString().split(";");

    for (const b of blockArray) {
      const [coords, state] = b.split(":");
      const [x, y, z] = coords.split(",");

      blockMap.set(getPosString(x, y, z), state);
    }
  }

  for (const b of blocks.split(";")) {
    const [coords, state] = b.split(":");
    const [x, y, z] = coords.split(",");

    blockMap.set(getPosString(x, y, z), state);
  }

  const newBlockArray = Array.from(blockMap.entries()).map((e) => {
    return `${e[0]}:${e[1]}`;
  });

  fs.writeFileSync(chunkFilePath, newBlockArray.join(";"));
});

function getPosString(x, y, z) {
  return `${x},${y},${z}`;
}
