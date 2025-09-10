const { Worker } = require("worker_threads");
const express = require("express");
const color = require("colors");
const fs = require("fs");
const app = express();

/*
  SETTINGS
*/
const port = 3000;
const workerCount = 2;
const publicChunks = false; // If the endpoint where a list of chunks should be enabled. This also toggles access to the content of said chunks
const chunkLeaderboard = true; // If the endpoint where a list of players and how many chunks they sent should be enabled
const worldSavePath = "world";

const workers = [];
let workerIndex = 0;

if (!fs.existsSync(worldSavePath)) fs.mkdirSync(worldSavePath);

app.use(express.text({ limit: "2mb" }));

app.post("/blocks/:dimension/:x/:z", (req, res) => {
  function checkMissing(value) {
    if (!value) {
      res.status(400).send(`A value is missing from your request`);
      return false;
    }

    return true;
  }

  function validNumberCheck(value) {
    if (!isParsableInt(value)) {
      res.status(400).send(`A value is not a parsable int`);
      return false;
    }

    return true;
  }

  let server = cleanString(req.get("Server-Address"));
  checkMissing(server);

  const version = cleanString(req.get("Client-Version"));
  checkMissing(version);

  const author = cleanString(req.get("Author"));
  checkMissing(author);

  const dimension = cleanString(
    req.params.dimension.replaceAll("minecraft:", "")
  );
  checkMissing(dimension);

  const chunkX = cleanString(req.params.x);
  checkMissing(chunkX);
  validNumberCheck(chunkX);

  const chunkZ = cleanString(req.params.z);
  checkMissing(chunkZ);
  validNumberCheck(chunkZ);

  const time = Date.now();

  const blocks = req.body;

  if (!blocks || blocks.length == 0) {
    console.log("No chunk data");
    console.log(blocks);
    return res.status(400).json("No chunk data");
  }

  if (server.endsWith("6b6t.org")) server = "6b6t.org";

  if (blocks.includes(",") && blocks.includes(":"))
    console.log(
      color.green(
        `Received ${
          blocks.split(";").length
        } ${chunkX},${chunkZ} from ${author} in ${dimension} (${server})`
      )
    );
  else {
    console.log(
      color.yellow(
        `Received fucked up ${chunkX},${chunkZ} from ${author} in ${dimension} (${server})`
      )
    );
    //console.log(blocks);
    res.status(400).send("what the fuck are you sending");
  }

  res.status(200).send();

  saveChunk({
    server,
    version,
    author,
    dimension,
    chunkX,
    chunkZ,
    blocks,
  });
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

if (chunkLeaderboard) {
  app.get("/chunkleaderboard", (req, res) => {
    let dimensions = new Map();

    for (const server of fs.readdirSync(worldSavePath)) {
      for (const dim of fs.readdirSync(`${worldSavePath}/${server}`)) {
        dimensions.set(dim, new Map());
        const chunkFiles = fs.readdirSync(`${worldSavePath}/${server}/${dim}`);
        const dimMap = dimensions.get(dim);

        for (const chunk of chunkFiles) {
          const { x, z, author, version, timestamp } = getChunkMetadata(
            chunk,
            2
          );
          const player = dimMap.get(author);

          if (player) dimMap.set(author, player + 1);
          else dimMap.set(author, 1);
        }
      }
    }

    let html = `
    <!DOCTYPE html>
    <html>
      <head>
        <title>Chunk Leaderboard</title>
        <style>
          body { font-family: Arial, sans-serif; background: #1e1e1e; color: #f0f0f0; }
          h1 { text-align: center; }
          .dimension { margin-bottom: 40px; }
          table { width: 60%; margin: 0 auto; border-collapse: collapse; box-shadow: 0 0 15px rgba(0,0,0,0.5); }
          th, td { padding: 12px 15px; border: 1px solid #444; text-align: center; }
          th { background: #333; color: #fff; }
          tr:nth-child(even) { background: #2a2a2a; }
          tr:nth-child(odd) { background: #252525; }
        </style>
      </head>
      <body>
        <h1>Chunk Leaderboard</h1>
  `;

    dimensions.forEach((players, dimension) => {
      html += `
      <div class="dimension">
        <h2 style="text-align:center;">${dimension}</h2>
        <table>
          <tr>
            <th>Player</th>
            <th>Chunks</th>
          </tr>
    `;

      const sortedPlayers = Array.from(players.entries()).sort(
        (a, b) => b[1] - a[1]
      );

      sortedPlayers.forEach(([player, count]) => {
        html += `
          <tr>
            <td>${player}</td>
            <td>${count}</td>
          </tr>
      `;
      });

      html += `</table></div>`;
    });

    html += `
      </body>
    </html>
  `;

    res.send(html);
  });

  app.post("/chunkleaderboard", (req, res) => {
    let dimensions = new Map();
    for (const server of fs.readdirSync(worldSavePath)) {
      for (const dim of fs.readdirSync(`${worldSavePath}/${server}`)) {
        dimensions.set(dim, new Map());
        const chunkFiles = fs.readdirSync(`${worldSavePath}/${server}/${dim}`);
        const dimMap = dimensions.get(dim);
        for (const chunk of chunkFiles) {
          const { x, z, author, version, timestamp } = getChunkMetadata(
            chunk,
            2
          );
          const player = dimMap.get(author);
          if (player) dimMap.set(author, player + 1);
          else dimMap.set(author, 1);
        }
      }
    }

    let response = {};
    dimensions.forEach((players, dimension) => {
      response[dimension] = {};
      players.forEach((count, player) => {
        response[dimension][player] = count;
      });
    });

    res.json(response);
  });
}

for (let i = 0; i < workerCount; i++) {
  console.log(color.green(`Starting worker ${i}`));
  workers.push(new Worker("./worker.js", { workerData: { worldSavePath } }));
}

app.listen(port, () => {
  console.log(`Server running on port ${port}`);
});

function saveChunk(data) {
  workers[workerIndex].postMessage(data);

  workerIndex = (workerIndex + 1) % workers.length;
}

function cleanString(str) {
  return str
    ? str
        .replaceAll(" ", "_")
        .replaceAll("/", "")
        .replaceAll("\\", "")
        .replaceAll(",", "")
        .replaceAll(";", "")
        .replaceAll(":", "")
    : null;
}

function isParsableInt(str) {
  if (typeof str !== "string") return false;
  const num = Number(str);

  return Number.isInteger(num) && str.trim() !== "";
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
      console.log(color.red(`Metadata version ${filename} doesnt exist`));
      return null;
  }
}
