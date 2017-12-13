import TypedValidator from '@workpop/typed-validation'

const exampleType = `
input SampleType {
  title: String
}
`

const testObject = {
  // title: 'Yo!',
}

// Create a TypedValidator instance
const Validator = new TypedValidator(exampleType)

console.log(Validator.validateOne(testObject, 'title'))
// true
