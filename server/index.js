const express = require("express");
const fs = require("fs");
const app = express();
const colors = require("colors");

const port = 3000;
const asyncSaving = true;
const worldSavePath = "world";
const publicChunks = false;

if (!fs.existsSync(worldSavePath)) fs.mkdirSync(worldSavePath);

app.use(express.json({ limit: "5mb" }));

app.post("/blocks/:dimension/:x/:z", (req, res) => {
  function checkMissing(value) {
    if (!value) {
      res.status(400).send(`A value is missing from your request`);
      return false;
    }

    return true;
  }

  const server = cleanString(req.get("Server-Address"));
  checkMissing(server);

  const version = cleanString(req.get("Client-Version"));
  checkMissing(version);

  const author = cleanString(req.get("Author"));
  checkMissing(author);

  const dimension = cleanString(req.params.dimension);
  checkMissing(dimension);

  const chunkX = cleanString(req.params.x);
  checkMissing(chunkX);

  const chunkZ = cleanString(req.params.z);
  checkMissing(chunkZ);

  const time = Date.now();

  const blocks = req.body;

  if (!blocks || blocks.length == 0) {
    return res.status(400).json("No chunk data");
  }

  if (Array.isArray(blocks))
    console.log(
      `Received ${
        req.body.length
      } ${chunkX},${chunkZ} from ${author} in ${dimension} (${server} ${req.get(
        "content-length"
      )})`
    );
  else {
    console.log(
      `Received ${chunkX},${chunkZ} from ${author} in ${dimension} (${server})`
    );
    console.log(blocks);
    return res.status(400).send(`what the fuck are you sending`);
  }

  const chunkPath = `${worldSavePath}/${server}/${dimension}`;
  if (!fs.existsSync(chunkPath)) fs.mkdirSync(chunkPath, { recursive: true });

  const chunkFilePath = `${chunkPath}/${chunkX} ${chunkZ} ${author} ${version}.json`;
  const blockMap = new Map();

  // If the chunk already exists read the contents
  if (fs.existsSync(chunkFilePath)) {
    const blockArray = JSON.parse(fs.readFileSync(chunkFilePath).toString());
    for (const b of blockArray) {
      blockMap.set(getPosString(b.x, b.y, b.z), b.state);
    }
  }

  // Write the new block data
  for (const b of req.body) {
    blockMap.set(getPosString(b.x, b.y, b.z), b.state);
  }

  const newBlockArray = Array.from(blockMap.entries()).map((e) => {
    return { ...fromPosStr(e[0]), state: e[1] };
  });
  if (asyncSaving) fs.writeFile(chunkFilePath, JSON.stringify(newBlockArray));
  else fs.writeFileSync(chunkFilePath, JSON.stringify(newBlockArray));

  res.status(200).send();
});

if (publicChunks) {
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
}

app.listen(port, () => {
  console.log(`Server running on port ${port}`);
});

function cleanString(str) {
  return str
    ? str.replaceAll(" ", "_").replaceAll("/", "").replaceAll("\\", "")
    : null;
}

function getPosString(x, y, z) {
  return `${x},${y},${z}`;
}

function fromPosStr(str) {
  const [x, y, z] = str.split(",");
  return { x, y, z };
}

function getChunkMetadata(filename, metadataVersion) {
  filename = filename.replace(".json", "");
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
