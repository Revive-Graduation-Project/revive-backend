import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  // Required for the Docker multi-stage build to produce a self-contained server bundle
  output: "standalone",
};

export default nextConfig;
