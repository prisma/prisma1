import { Box, Color } from 'ink'
import InkTextInput from 'ink-text-input'
import * as React from 'react'

interface TextInputProps {
  label: string
  value: string
  onChange: (value: string) => void
  placeholder?: string
  focus: boolean
  mask?: string
}

export const TextInput: React.SFC<TextInputProps> = ({
  value,
  onChange,
  label,
  focus,
  placeholder,
  mask,
}) => (
  <Box>
    <Box marginRight={1}>{focus ? <Color green>{label}</Color> : label}</Box>
    <InkTextInput
      value={value}
      placeholder={placeholder}
      onChange={value => {
        if (focus) {
          onChange(value)
        }
      }}
      showCursor={focus}
      mask={mask}
    />
  </Box>
)
