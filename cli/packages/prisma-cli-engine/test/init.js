require('nock').disableNetConnect()
jest.unmock('fs-extra')
process.setMaxListeners(0)
global.columns = 80
global.testing = true