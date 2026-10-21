import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
// 只检查文档位置和本地链接，不连接数据库或读取凭据。
const root = fileURLToPath(new URL('../', import.meta.url));
const docs = path.join(root, 'docs') + path.sep;
const excluded = new Set(['.git', '.local', '.idea', 'target', 'node_modules']);
const failures = [];
let count = 0;
function walk(dir) {
  for (const item of fs.readdirSync(dir, { withFileTypes: true })) {
    if (excluded.has(item.name)) continue;
    const file = path.join(dir, item.name);
    if (item.isDirectory()) { walk(file); continue; }
    if (!item.name.endsWith('.md')) continue;
    count++;
    if (!file.startsWith(docs)) failures.push(path.relative(root, file) + ': 文档必须放入仓库根 docs/');
    const body = fs.readFileSync(file, 'utf8').replace(/```[\s\S]*?```/g, '');
    for (const match of body.matchAll(/\[[^\]\n]+\]\(([^)]+)\)/g)) {
      const link = match[1];
      if (/^(https?:|mailto:|#)/.test(link)) continue;
      const target = path.resolve(path.dirname(file), decodeURIComponent(link.split('#')[0]));
      if (!fs.existsSync(target)) failures.push(path.relative(root, file) + ': 无效链接 ' + link);
    }
  }
}
walk(root);
console.log(JSON.stringify({ markdownFiles: count, failures }, null, 2));
if (failures.length) process.exitCode = 1;
