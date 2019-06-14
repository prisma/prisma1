import * as React from 'react'
import { Prompt } from '../../prompt-lib/BoxPrompt'
import { PromptState } from '../InteractivePrompt'
import { ActionType } from '../reducer'
import { Steps, stepsToElements } from '../steps-definition'

export function renderSelectDatabaseType(dispatch: React.Dispatch<ActionType>, state: PromptState) {
  return (
    <Prompt
      key={Steps.SELECT_DATABASE_TYPE}
      title="What kind of database do you want to introspect?"
      elements={stepsToElements[Steps.SELECT_DATABASE_TYPE]()}
      onSubmit={({ selectedValue }) => {
        dispatch({
          type: 'choose_db',
          payload: selectedValue,
        })
      }}
      formValues={state.credentials}
      withBackButton={false}
    />
  )
}
