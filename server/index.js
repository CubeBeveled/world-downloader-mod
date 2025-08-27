const express = require("express");
const fs = require("fs");
const app = express();

const port = 3000;
const savePath = "world";

if (!fs.existsSync(savePath)) fs.mkdirSync(savePath);

app.post("/newchunk/:server/:author/:dimension/:x/:z", (req, res) => {
  const server = req.params.server;
  const dimension = req.params.dimension.replace("minecraft:","");
  const author = req.params.author;
  const x = req.params.x;
  const z = req.params.z;

  console.log(`Received chunk ${x},${z} from ${author} in ${dimension} (${server})`)

  const buffer = Buffer.from(req.body, 'base64');

  const chunkPath = `${savePath}/${server}/${dimension}`
  if (!fs.existsSync(chunkPath)) fs.mkdirSync(chunkPath,{ recursive: true });
  fs.writeFileSync(`${chunkPath}/${x}_${z}_${author.replace("_", "--")}`, buffer);

  res.status(200);
});

app.listen(port, () => {
  console.log(`Server running on port ${port}`);
});