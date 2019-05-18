module.exports = {
  lintOnSave: false,
  outputDir: 'build/resources/main',
  assetsDir: 'public',
    pages: {
      app: {
        entry: './src/main/ts/main.ts'
      },
      submission: {
        entry: './src/main/ts/review.ts'
      }
    }
}
