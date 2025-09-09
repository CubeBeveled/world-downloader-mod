const { parentPort, workerData } = require("worker_threads");
const fs = require("fs");

parentPort.on("message", (data) => {
  const { server, version, author, dimension, chunkX, chunkZ, blocks } = data;

  const chunkPath = `${workerData.worldSavePath}/${server}/${dimension}`;
  if (!fs.existsSync(chunkPath)) fs.mkdirSync(chunkPath, { recursive: true });

  const chunkFilePath = `${chunkPath}/${chunkX} ${chunkZ} ${author} ${version}.json`;
  const blockMap = new Map();

  if (fs.existsSync(chunkFilePath)) {
    const blockArray = JSON.parse(fs.readFileSync(chunkFilePath).toString());

    for (const b of blockArray) {
      blockMap.set(getPosString(b.x, b.y, b.z), b.state);
    }
  }

  for (const b of blocks) {
    blockMap.set(getPosString(b.x, b.y, b.z), b.state);
  }

  const newBlockArray = Array.from(blockMap.entries()).map((e) => {
    return { ...fromPosStr(e[0]), state: e[1] };
  });

  fs.writeFileSync(chunkFilePath, JSON.stringify(newBlockArray));
});
