import { Table, Column, TableRelation } from './types/common'
import { SDL, GQLType, GQLField } from './types/graphql'
import * as _ from 'lodash'
const pluralize = require('pluralize')
const upperCamelCase = require('uppercamelcase');
