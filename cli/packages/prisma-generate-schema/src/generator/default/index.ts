import { IGenerators, TypeRegistry, SchemaGeneratorBase } from '../generator'
import ModelCreateInputGenerator from './mutation/create/modelCreateInputGenerator'
import ModelCreateOneInputGenerator from './mutation/create/modelCreateOneInputGenerator'
import ModelCreateManyInputGenerator from './mutation/create/modelCreateManyInputGenerator'
import ModelCreateOneWithoutRelatedInputGenerator from './mutation/create/modelCreateOneWithoutRelatedInputGenerator'
import ModelCreateWithoutRelatedInputGenerator from './mutation/create/modelCreateWithoutRelatedInputGenerator.ts'
import ModelCreateManyWithoutRelatedInputGenerator from './mutation/create/modelCreateManyWithoutRelatedInputGenerator'
import ScalarListCreateInput from './mutation/create/scalarListCreateInput'
import ModelUpdateInputGenerator from './mutation/update/modelUpdateInputGenerator'
import ModelUpdateDataInputGenerator from './mutation/update/modelUpdateDataInputGenerator'
import ModelUpdateManyDataInputGenerator from './mutation/update/modelUpdateManyDataInputGenerator'
import ModelUpdateOneInputTypeGenerator from './mutation/update/modelUpdateOneInputGenerator'
import ModelUpdateOneRequiredInputTypeGenerator from './mutation/update/modelUpdateOneRequiredInputGenerator'
import ModelUpdateManyInputTypeGenerator from './mutation/update/modelUpdateManyInputGenerator'
import ModelUpdateManyMutationInputTypeGenerator from './mutation/update/modelUpdateManyMutationInputGenerator'
import ModelUpdateWithoutRelatedDataInputGenerator from './mutation/update/modelUpdateWithoutRelatedDataInputGenerator'
import ModelUpdateOneWithoutRelatedInputTypeGenerator from './mutation/update/modelUpdateOneWithoutRelatedInputGenerator'
import ModelUpdateOneRequiredWithoutRelatedInputTypeGenerator from './mutation/update/modelUpdateOneRequiredWithoutRelatedInputGenerator'
import ModelUpdateManyWithoutRelatedInputTypeGenerator from './mutation/update/modelUpdateManyWithoutRelatedInputGenerator'
import ModelUpdateWithWhereUniqueNestedInput from './mutation/update/modelUpdateWithWhereUniqueNestedInputGenerator'
import ModelUpdateManyWithWhereNestedInput from './mutation/update/modelUpdateManyWithWhereNestedInputGenerator'
import ModelUpdateWithWhereUniqueWithoutRelatedInput from './mutation/update/modelUpdateWithWhereUniqueWithoutRelatedInputGenerator'
import ScalarListUpdateInput from './mutation/update/scalarListUpdateInput'
import ModelUpsertNestedInputGenerator from './mutation/upsert/modelUpsertNestedInputGenerator'
import ModelUpsertWithWhereUniqueWithoutRelatedInputGenerator from './mutation/upsert/modelUpsertWithWhereUniqueWithoutRelatedInputGenerator'
import ModelUpsertWithoutRelatedInputGenerator from './mutation/upsert/modelUpsertWithoutRelatedInputGenerator'
import ModelUpsertWithWhereUniqueNestedInputGenerator from './mutation/upsert/modelUpsertWithWhereUniqueNestedInputGenerator'
import MutationGenerator from './mutation/mutationGenerator'
import SubscriptionGenerator from './subscription/subscriptionGenerator'
import ModelWhereInputGenerator from './query/modelWhereInputGenerator'
import ModelScalarWhereInputGenerator from './query/modelScalarWhereInputGenerator'
import ModelWhereUniqueInputGenerator from './query/modelWhereUniqueInputGenerator'
import ModelOrderByInputGenerator from './query/modelOrderByInputGenerator'
import ModelEdgeGenerator from './query/modelEdgeGenerator'
import ModelConnectionGenerator from './query/modelConnectionGenerator'
import SchemaGenerator from './schemaGenerator'
import QueryGenerator from './query/queryGenerator'
import AggregateModelGenerator from './query/aggregateModelGenerator'
import PageInfoGenerator from './query/pageInfoGenerator'
import ModelGenerator from './query/modelGenerator'
import OneQueryArgumentsGenerator from './query/oneQueryArgumentsGenerator'
import ManyQueryArgumentsGenerator from './query/manyQueryArgumentsGenerator'
import UniqueQueryArgumentsGenerator from './query/uniqueQueryArgumentsGenerator'
import NodeGenerator from './query/nodeGenerator'
import ModelSubscriptionPayloadGenerator from './subscription/modelSubscriptionPayloadGenerator'
import ModelSubscriptionWhereInputGenerator from './subscription/modelSubscriptionWhereInputGenerator'
import MutationTypeGenerator from './subscription/mutationTypeGenerator'
import ModelPreviousValuesGenerator from './subscription/modelPreviousValuesGenerator'
import BatchPayloadGenerator from './mutation/updateMany/batchPayloadGenerator'
import ModelEnumTypeGenerator from './scalar/modelEnumTypeGenerator'
import ScalarTypeGenerator from './scalar/scalarTypeGenerator'

/**
 * This class represents a collection of all existing generators. The generators used
 * here depend on each other.
 */
export default class DefaultGenerator implements IGenerators {
  typeRegistry = new TypeRegistry()
  modelCreateInput = new ModelCreateInputGenerator(this.typeRegistry, this)
  modelCreateOneInput = new ModelCreateOneInputGenerator(
    this.typeRegistry,
    this,
  )
  modelCreateManyInput = new ModelCreateManyInputGenerator(
    this.typeRegistry,
    this,
  )
  modelCreateWithoutRelatedInput = new ModelCreateWithoutRelatedInputGenerator(
    this.typeRegistry,
    this,
  )
  modelCreateOneWithoutRelatedInput = new ModelCreateOneWithoutRelatedInputGenerator(
    this.typeRegistry,
    this,
  )
  modelCreateManyWithoutRelatedInput = new ModelCreateManyWithoutRelatedInputGenerator(
    this.typeRegistry,
    this,
  )
  scalarListCreateInput = new ScalarListCreateInput(this.typeRegistry, this)

  modelUpdateInput = new ModelUpdateInputGenerator(this.typeRegistry, this)
  modelUpdateManyDataInput = new ModelUpdateManyDataInputGenerator(
    this.typeRegistry,
    this,
  )
  modelUpdateDataInput = new ModelUpdateDataInputGenerator(
    this.typeRegistry,
    this,
  )
  modelUpdateOneInput = new ModelUpdateOneInputTypeGenerator(
    this.typeRegistry,
    this,
  )
  modelUpdateOneRequiredInput = new ModelUpdateOneRequiredInputTypeGenerator(
    this.typeRegistry,
    this,
  )
  modelUpdateManyInput = new ModelUpdateManyInputTypeGenerator(
    this.typeRegistry,
    this,
  )
  modelUpdateManyMutationInput = new ModelUpdateManyMutationInputTypeGenerator(
    this.typeRegistry,
    this,
  )
  modelUpdateWithoutRelatedDataInput = new ModelUpdateWithoutRelatedDataInputGenerator(
    this.typeRegistry,
    this,
  )
  modelUpdateOneWithoutRelatedInput = new ModelUpdateOneWithoutRelatedInputTypeGenerator(
    this.typeRegistry,
    this,
  )
  modelUpdateOneRequiredWithoutRelatedInput = new ModelUpdateOneRequiredWithoutRelatedInputTypeGenerator(
    this.typeRegistry,
    this,
  )
  modelUpdateManyWithoutRelatedInput = new ModelUpdateManyWithoutRelatedInputTypeGenerator(
    this.typeRegistry,
    this,
  )
  modelUpdateWithWhereUniqueWithoutRelatedInput = new ModelUpdateWithWhereUniqueWithoutRelatedInput(
    this.typeRegistry,
    this,
  )
  modelUpdateWithWhereUniqueNestedInput = new ModelUpdateWithWhereUniqueNestedInput(
    this.typeRegistry,
    this,
  )
  modelUpdateManyWithWhereNestedInput =new ModelUpdateManyWithWhereNestedInput(
    this.typeRegistry,
    this,
  )
  scalarListUpdateInput = new ScalarListUpdateInput(this.typeRegistry, this)

  modelUpsertNestedInput = new ModelUpsertNestedInputGenerator(
    this.typeRegistry,
    this,
  )
  modelUpsertWithWhereUniqueWithoutRelatedInput = new ModelUpsertWithWhereUniqueWithoutRelatedInputGenerator(
    this.typeRegistry,
    this,
  )
  modelUpsertWithoutRelatedInput = new ModelUpsertWithoutRelatedInputGenerator(
    this.typeRegistry,
    this,
  )
  modelUpsertWithWhereUniqueNestedInput = new ModelUpsertWithWhereUniqueNestedInputGenerator(
    this.typeRegistry,
    this,
  )

  modelWhereUniqueInput = new ModelWhereUniqueInputGenerator(
    this.typeRegistry,
    this,
  )
  modelWhereInput = new ModelWhereInputGenerator(this.typeRegistry, this)
  modelScalarWhereInput = new ModelScalarWhereInputGenerator(this.typeRegistry, this)
  modelOrderByInput = new ModelOrderByInputGenerator(this.typeRegistry, this)
  modelConnection = new ModelConnectionGenerator(this.typeRegistry, this)
  modelEdge = new ModelEdgeGenerator(this.typeRegistry, this)
  aggregateModel = new AggregateModelGenerator(this.typeRegistry, this)
  pageInfo = new PageInfoGenerator(this.typeRegistry, this)
  model = new ModelGenerator(this.typeRegistry, this)
  oneQueryArguments = new OneQueryArgumentsGenerator(this.typeRegistry, this)
  manyQueryArguments = new ManyQueryArgumentsGenerator(this.typeRegistry, this)
  uniqueQueryArguments = new UniqueQueryArgumentsGenerator(
    this.typeRegistry,
    this,
  )
  node = new NodeGenerator(this.typeRegistry, this)

  batchPayload = new BatchPayloadGenerator(this.typeRegistry, this)

  modelSubscriptionPayload = new ModelSubscriptionPayloadGenerator(
    this.typeRegistry,
    this,
  )
  modelSubscriptionWhereInput = new ModelSubscriptionWhereInputGenerator(
    this.typeRegistry,
    this,
  )
  mutationType = new MutationTypeGenerator(this.typeRegistry, this)
  modelPreviousValues = new ModelPreviousValuesGenerator(
    this.typeRegistry,
    this,
  )

  query = new QueryGenerator(this.typeRegistry, this)
  mutation = new MutationGenerator(this.typeRegistry, this)
  subscription = new SubscriptionGenerator(this.typeRegistry, this)

  schema = new SchemaGenerator(this.typeRegistry, this)
  modelEnumTypeGenerator = new ModelEnumTypeGenerator(this.typeRegistry, this)
  scalarTypeGenerator = new ScalarTypeGenerator(this.typeRegistry, this)
}
