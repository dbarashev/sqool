import Vue from 'vue';

declare global {
  interface Window { userName: string | undefined; }
}
