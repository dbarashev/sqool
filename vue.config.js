const path = require('path');

module.exports = {
  devServer: {
    disableHostCheck: true
  },
  filenameHashing: false,
  lintOnSave: false,
  publicPath: '/',
  outputDir: 'src/main/resources/public',
  pages: {
      admin: {
        entry: './src/main/ts/admin/main.ts',
        template: './src/main/ts/template/dashboard.html',
        filename: 'dashboard'
      },
      user: './src/main/ts/user/main.ts',
      signup: './src/main/ts/auth/Signup.ts'
  },
    configureWebpack: {
        devServer: {
            headers: { "Access-Control-Allow-Origin": "*" }
        },
        resolve: {
            alias: {
                'vue$': 'vue/dist/vue.esm.js', // 'vue/dist/vue.common.js' for webpack 1
                '@': path.resolve(__dirname, "src/main/ts/")
            }
        },
        // optimization: {
        //     // runtimeChunk: 'single',
        //     splitChunks: {
        //         chunks: 'all',
        //         maxInitialRequests: Infinity,
        //         cacheGroups: {
        //             vendor: {
        //                 test: /[\\/]node_modules[\\/](firebase|qunit|sinon).*[\\/]/,
        //                 name(module) {
        //                     // get the name. E.g. node_modules/packageName/not/this/part.js
        //                     // or node_modules/packageName
        //                     const packageName = module.context.match(/[\\/]node_modules[\\/](.*?)([\\/]|$)/)[1];
        //
        //                     // npm package names are URL-safe, but some servers don't like @ symbols
        //                     return `npm.${packageName.replace('@', '')}`;
        //                 },
        //                 minSize: 30,
        //             },
        //         },
        //     },
        // },
    }
};
