import test from 'ava'
import TestResolver from '../src/resolvers/TestResolver'
const fetchMock = require('fetch-mock')
const debug = require('debug')('graphcool')
import 'isomorphic-fetch'
import pushCommand from '../src/commands/push'
import {systemAPIEndpoint} from '../src/utils/constants'

test.afterEach(() => {
  fetchMock.reset()
})

/*
 * Test succeeding schema migration and verify updated project info is stored in in ./project.graphcool
 */
test('Succeeding project creation', async t => {

  // configure HTTP mocks
  fetchMock.post(systemAPIEndpoint, JSON.parse(''))

})
