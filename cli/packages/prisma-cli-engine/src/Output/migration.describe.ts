import { Config } from '../Config'
import { Output } from './index'
import { MigrationPrinter } from './migration'
import { steps } from './fixtures/steps'

const config = new Config({ mock: false })
const out = new Output(config)
const printer = new MigrationPrinter(out)

printer.printMessages(steps)
