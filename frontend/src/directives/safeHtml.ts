// Direttiva v-safe-html: come v-html ma con sanificazione DOMPurify.
import type {Directive} from 'vue'
import {sanitizeHtml} from '../function/sanitize'

function set(el: HTMLElement, value: unknown) {
    el.innerHTML = sanitizeHtml(value as string)
}

export const vSafeHtml: Directive<HTMLElement, unknown> = {
    mounted(el, binding) {
        set(el, binding.value)
    },
    updated(el, binding) {
        if (binding.value !== binding.oldValue) set(el, binding.value)
    },
}
