import { Box, Color } from 'ink'
import * as React from 'react'
import { BACK_SYMBOL } from './helpers'
import { Spinner } from './Spinner'
import { SpinnerState } from './types'
import { useStdin } from './useStdin'
import figures = require('figures')

interface Props {
  label: string
  spinnerState: SpinnerState | undefined
  value?: any
  focus: boolean
  description?: string
  isBackButton?: boolean
  onSelect: (value?: any) => void
}

function renderSelectIndicator(
  spinnerState: SpinnerState | undefined,
  isBackButton: boolean,
) {
  if (isBackButton) {
    return BACK_SYMBOL
  }

  if (spinnerState === 'running') {
    return <Spinner />
  }
  if (spinnerState === 'failed') {
    return <Color red>{figures.cross}</Color>
  }
  if (spinnerState === 'succeeded') {
    return <Color green>{figures.tick}</Color>
  }

  return figures.pointer
}

const SelectIndicator: React.FC<{
  spinnerState: SpinnerState | undefined
  isBackButton: boolean
}> = props => (
  <Box marginRight={1}>
    {renderSelectIndicator(props.spinnerState, props.isBackButton)}
  </Box>
)

export const SelectItem: React.FC<Props> = props => {
  const indicator = (
    <SelectIndicator
      spinnerState={props.spinnerState}
      isBackButton={props.isBackButton!}
    />
  )

  useStdin(
    async actionKey => {
      if (props.focus && actionKey === 'submit') {
        await props.onSelect(props.value)
      }
    },
    [props.focus, props.value],
  )

  return (
    <Box>
      {props.focus ? <Color green>{indicator}</Color> : indicator}
      <Box marginLeft={1}>
        {props.focus ? (
          <Color green>{props.label.padEnd(20)}</Color>
        ) : (
          props.label.padEnd(20)
        )}
      </Box>
      {props.description && <Color dim>{props.description}</Color>}
    </Box>
  )
}

SelectItem.defaultProps = {
  isBackButton: false,
}
