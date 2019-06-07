import * as figures from 'figures'
import { Box, BoxProps, Color } from 'ink'
import * as React from 'react'
import { useStdin } from './useStdin'

interface Props extends BoxProps {
  label: string
  checked: boolean
  focus: boolean
  onChange: (value: boolean) => void
}

export const Checkbox: React.FC<Props> = props => {
  const symbol = props.checked ? figures.radioOn : figures.radioOff
  const { label, checked, focus, onChange, ...rest } = props

  useStdin(
    actionKey => {
      if (focus && actionKey === 'submit') {
        onChange(!checked)
      }
    },
    [checked, focus],
  )

  return (
    <Box {...rest}>
      {focus ? <Color green>{symbol}</Color> : symbol}
      <Box marginLeft={1}>{focus ? <Color green>{label}</Color> : label}</Box>
    </Box>
  )
}
