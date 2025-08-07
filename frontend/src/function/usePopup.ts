// src/function/usePopup.ts
import {markRaw, ref, shallowRef} from 'vue'

/** stato condiviso **/
const isVisible = ref(false)
const dynamicComp = shallowRef<any>(null)    // <-- shallowRef qui
const dynamicProps = ref<Record<string, any>>({})
const isClosable = ref(true)
let autoCloseId: ReturnType<typeof setTimeout> | null = null

function openPopup(
    component: any,
    props: Record<string, any> = {},
    options: { autoClose?: number; closable?: boolean } = {}
) {
    // pulisco l'eventuale timer
    if (autoCloseId) {
        clearTimeout(autoCloseId)
        autoCloseId = null
    }

    // marchia il componente come “raw” per evitare warning/render error
    dynamicComp.value = markRaw(component)
    dynamicProps.value = props
    isClosable.value = options.closable ?? true
    isVisible.value = true

    if (options.autoClose && options.autoClose > 0) {
        autoCloseId = setTimeout(() => {
            closePopup()
            autoCloseId = null
        }, options.autoClose)
    }
}

function closePopup() {
    if (autoCloseId) {
        clearTimeout(autoCloseId)
        autoCloseId = null
    }
    isVisible.value = false
}

export default function usePopup() {
    return {
        isVisible,
        dynamicComp,
        dynamicProps,
        isClosable,
        openPopup,
        closePopup
    }
}
