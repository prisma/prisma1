import { Validator } from './Validator'
import * as fs from 'fs-extra'

const types = fs.readFileSync(
  __dirname + '/fixtures/basic/types.graphql',
  'utf-8',
)

const validator = new Validator(types, {
  Post: {
    comments: 'comments.id',
  },
  Comment: {
    post: 'postId',
  },
})

const post = {
  _typeName: 'Post',
  id: '0',
  title: 'This is a title',
  description: 'And I have a description',
}

validator.validateNode(post)
