module.exports = {
  lintOnSave: false,
  outputDir: 'build/resources/main',
  assetsDir: 'public',

  configureWebpack: config => {
     config.entry = {
       app: [
         './src/main/ts/main.ts'
       ]
     }
  }
}
