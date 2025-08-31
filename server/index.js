const express = require("express");
const fs = require("fs");
const app = express();
const colors = require("colors");

const port = 3000;
const worldSavePath = "world";

if (!fs.existsSync(worldSavePath)) fs.mkdirSync(worldSavePath);

app.use(express.raw({ type: "*/*" }));

app.post("/newchunk/:server/:author/:version/:dimension/:x/:z", (req, res) => {
  const server = cleanString(req.params.server);
  const dimension = cleanString(req.params.dimension).replace("minecraft:", "");
  const author = cleanString(req.params.author);
  const version = cleanString(req.params.version);
  const x = cleanString(req.params.x);
  const z = cleanString(req.params.z);
  const time = Date.now();

  const chunkData = req.body;

  if (!chunkData || chunkData.length == 0) {
    return res.status(400).json({ error: "No chunk data received" });
  }

  console.log(
    `Received chunk ${x},${z} from ${author} in ${dimension} (${server})`
  );

  const chunkPath = `${worldSavePath}/${server}/${dimension}`;
  if (!fs.existsSync(chunkPath)) fs.mkdirSync(chunkPath, { recursive: true });
  fs.writeFileSync(
    `${chunkPath}/${x} ${z} ${author} ${version}.bin`,
    chunkData
  );

  res.status(200).send();
});

app.use("/chunk", express.static(worldSavePath));
app.get("/chunks", (req, res) => {
  let dimensions = {};

  for (const server of fs.readdirSync(worldSavePath)) {
    for (const dim of fs.readdirSync(`${worldSavePath}/${server}`)) {
      dimensions[dim] = [];
      const chunkFiles = fs.readdirSync(`${worldSavePath}/${server}/${dim}`);

      for (const chunk of chunkFiles) {
        const { x, z, author, timestamp } = getChunkMetadata(chunk, 2);

        dimensions[dim].push({
          x,
          z,
          author,
          timestamp,
          path: `/${server}/${dim}/${chunk}`,
        });
      }
    }
  }

  res.json(dimensions);
});

app.listen(port, () => {
  console.log(`Server running on port ${port}`);
});

function cleanString(str) {
  console.log;
  return str.replaceAll(" ", "_").replaceAll("/", "");
}

function getChunkMetadata(filename, metadataVersion) {
  filename = filename.replace(".bin", "");
  let metadata;

  switch (metadataVersion) {
    case 1:
      metadata = filename.split("_");
      return { x: metadata[0], z: metadata[1], author: metadata[2] };

    case 2:
      metadata = filename.split(" ");
      return {
        x: metadata[0],
        z: metadata[1],
        author: metadata[2],
        version: metadata[3],
        timestamp: metadata[4],
      };

    default:
      console.log(colors.red(`Metadata version ${filename} doesnt exist`));
      return null;
  }
}
