module.exports = {
  preset: 'ts-jest',
  testEnvironment: 'node',
  modulePathIgnorePatterns: ['tmp/', 'build/', 'dist/', 'generator/'],
  watchPathIgnorePatterns: ['tmp/', 'build/', 'dist/', 'generator/'],
}
