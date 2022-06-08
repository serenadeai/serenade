import { core } from "../../gen/core";

export interface Chunk {
  audioSize: number;
  executed: number;
  id: string;
  reverted: number;
  silence: number;
  response?: core.ICommandsResponse;
  revertedResponse?: core.ICommandsResponse;
}

export class ChunkQueue {
  private maximumSize: number = 50;
  private queue: Chunk[] = [];

  add(id: string) {
    this.queue.unshift({
      audioSize: 0,
      executed: 0,
      id,
      reverted: 0,
      silence: 0,
    });

    while (this.queue.length > this.maximumSize) {
      this.queue.pop();
    }
  }

  clear() {
    this.queue = [];
  }

  getChunk(id: string): Chunk | null {
    const index = this.indexOf(id);
    return index > -1 ? this.getIndex(index) : null;
  }

  getIndex(index: number) {
    return this.queue[index];
  }

  indexOf(id: string): number {
    return this.queue.findIndex((e: Chunk) => e.id == id);
  }

  remove(id: string) {
    this.queue = this.queue.filter((e: Chunk) => e.id != id);
  }

  size(): number {
    return this.queue.length;
  }
}
