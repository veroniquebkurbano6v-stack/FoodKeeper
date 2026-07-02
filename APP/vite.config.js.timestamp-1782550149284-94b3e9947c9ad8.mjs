// vite.config.js
import { defineConfig } from "file:///C:/Users/H1314/Desktop/TRAE%E9%A3%9F%E5%BA%93%E7%AE%A1%E5%AE%B6/APP/node_modules/vite/dist/node/index.js";
import vue from "file:///C:/Users/H1314/Desktop/TRAE%E9%A3%9F%E5%BA%93%E7%AE%A1%E5%AE%B6/APP/node_modules/@vitejs/plugin-vue/dist/index.mjs";
import { fileURLToPath, URL } from "node:url";
import { viteSingleFile } from "file:///C:/Users/H1314/Desktop/TRAE%E9%A3%9F%E5%BA%93%E7%AE%A1%E5%AE%B6/APP/node_modules/vite-plugin-singlefile/dist/esm/index.js";
var __vite_injected_original_import_meta_url = "file:///C:/Users/H1314/Desktop/TRAE%E9%A3%9F%E5%BA%93%E7%AE%A1%E5%AE%B6/APP/vite.config.js";
var vite_config_default = defineConfig({
  plugins: [vue(), viteSingleFile()],
  resolve: {
    alias: {
      "@": fileURLToPath(new URL("./src", __vite_injected_original_import_meta_url))
    }
  },
  build: {
    target: "esnext",
    assetsInlineLimit: 1e8,
    chunkSizeWarningLimit: 1e8,
    cssCodeSplit: false,
    rollupOptions: {
      output: {
        inlineDynamicImports: true
      }
    }
  }
});
export {
  vite_config_default as default
};
//# sourceMappingURL=data:application/json;base64,ewogICJ2ZXJzaW9uIjogMywKICAic291cmNlcyI6IFsidml0ZS5jb25maWcuanMiXSwKICAic291cmNlc0NvbnRlbnQiOiBbImNvbnN0IF9fdml0ZV9pbmplY3RlZF9vcmlnaW5hbF9kaXJuYW1lID0gXCJDOlxcXFxVc2Vyc1xcXFxIMTMxNFxcXFxEZXNrdG9wXFxcXFRSQUVcdTk4REZcdTVFOTNcdTdCQTFcdTVCQjZcXFxcQVBQXCI7Y29uc3QgX192aXRlX2luamVjdGVkX29yaWdpbmFsX2ZpbGVuYW1lID0gXCJDOlxcXFxVc2Vyc1xcXFxIMTMxNFxcXFxEZXNrdG9wXFxcXFRSQUVcdTk4REZcdTVFOTNcdTdCQTFcdTVCQjZcXFxcQVBQXFxcXHZpdGUuY29uZmlnLmpzXCI7Y29uc3QgX192aXRlX2luamVjdGVkX29yaWdpbmFsX2ltcG9ydF9tZXRhX3VybCA9IFwiZmlsZTovLy9DOi9Vc2Vycy9IMTMxNC9EZXNrdG9wL1RSQUUlRTklQTMlOUYlRTUlQkElOTMlRTclQUUlQTElRTUlQUUlQjYvQVBQL3ZpdGUuY29uZmlnLmpzXCI7aW1wb3J0IHsgZGVmaW5lQ29uZmlnIH0gZnJvbSAndml0ZSdcbmltcG9ydCB2dWUgZnJvbSAnQHZpdGVqcy9wbHVnaW4tdnVlJ1xuaW1wb3J0IHsgZmlsZVVSTFRvUGF0aCwgVVJMIH0gZnJvbSAnbm9kZTp1cmwnXG5pbXBvcnQgeyB2aXRlU2luZ2xlRmlsZSB9IGZyb20gJ3ZpdGUtcGx1Z2luLXNpbmdsZWZpbGUnXG5cbmV4cG9ydCBkZWZhdWx0IGRlZmluZUNvbmZpZyh7XG4gIHBsdWdpbnM6IFt2dWUoKSwgdml0ZVNpbmdsZUZpbGUoKV0sXG4gIHJlc29sdmU6IHtcbiAgICBhbGlhczoge1xuICAgICAgJ0AnOiBmaWxlVVJMVG9QYXRoKG5ldyBVUkwoJy4vc3JjJywgaW1wb3J0Lm1ldGEudXJsKSlcbiAgICB9XG4gIH0sXG4gIGJ1aWxkOiB7XG4gICAgdGFyZ2V0OiAnZXNuZXh0JyxcbiAgICBhc3NldHNJbmxpbmVMaW1pdDogMTAwMDAwMDAwLFxuICAgIGNodW5rU2l6ZVdhcm5pbmdMaW1pdDogMTAwMDAwMDAwLFxuICAgIGNzc0NvZGVTcGxpdDogZmFsc2UsXG4gICAgcm9sbHVwT3B0aW9uczoge1xuICAgICAgb3V0cHV0OiB7XG4gICAgICAgIGlubGluZUR5bmFtaWNJbXBvcnRzOiB0cnVlXG4gICAgICB9XG4gICAgfVxuICB9XG59KSJdLAogICJtYXBwaW5ncyI6ICI7QUFBdVUsU0FBUyxvQkFBb0I7QUFDcFcsT0FBTyxTQUFTO0FBQ2hCLFNBQVMsZUFBZSxXQUFXO0FBQ25DLFNBQVMsc0JBQXNCO0FBSDBKLElBQU0sMkNBQTJDO0FBSzFPLElBQU8sc0JBQVEsYUFBYTtBQUFBLEVBQzFCLFNBQVMsQ0FBQyxJQUFJLEdBQUcsZUFBZSxDQUFDO0FBQUEsRUFDakMsU0FBUztBQUFBLElBQ1AsT0FBTztBQUFBLE1BQ0wsS0FBSyxjQUFjLElBQUksSUFBSSxTQUFTLHdDQUFlLENBQUM7QUFBQSxJQUN0RDtBQUFBLEVBQ0Y7QUFBQSxFQUNBLE9BQU87QUFBQSxJQUNMLFFBQVE7QUFBQSxJQUNSLG1CQUFtQjtBQUFBLElBQ25CLHVCQUF1QjtBQUFBLElBQ3ZCLGNBQWM7QUFBQSxJQUNkLGVBQWU7QUFBQSxNQUNiLFFBQVE7QUFBQSxRQUNOLHNCQUFzQjtBQUFBLE1BQ3hCO0FBQUEsSUFDRjtBQUFBLEVBQ0Y7QUFDRixDQUFDOyIsCiAgIm5hbWVzIjogW10KfQo=
