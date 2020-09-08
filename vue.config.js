module.exports = {
  lintOnSave: false,
  outputDir: 'build/resources/main',
  assetsDir: 'public',
    pages: {
      admin: './src/main/ts/admin/main.ts',
      user: './src/main/ts/user/main.ts',
      signup: './src/main/ts/auth/Signup.ts'
    }
};
