/*
 * Copyright 2019-2020 StreamThoughts.
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
<template>
  <div>
    <div class="vue-json-editor"></div>
  </div>
</template>

<script>
import 'jsoneditor/dist/jsoneditor.min.css';
import JsonEditor from 'jsoneditor/dist/jsoneditor.min.js';

export default {
  props: {
    value: [String, Number, Object, Array]
  },
  watch: {
    value: {
      immediate: true,
      async handler(val) {
        if (!this.internalChange) {
          await this.setEditor(val);
        }
      },
      deep: true
    }
  },
  data() {
    return {
      editor: null,
      error: false,
      json: this.value,
      internalChange: false
    };
  },
  mounted() {
    this.initEditor();
  },
  beforeDestroy() {
    this.destroyEditor();
  },
  methods: {
    async setEditor(value) {
      if (this.editor) this.editor.set(value);
    },

    destroyEditor() {
      if (this.editor) {
        this.editor.destroy();
        this.editor = null;
      }
    },
    initEditor() {
      let self = this;
      let options = {
        indentation: 2,
        mainMenuBar: false,
        mode: 'code',
        onChange() {
          try {
            let json = self.editor.get();
            self.json = json;
            self.$emit("json-change", json);
            self.internalChange = true;
            self.$emit("input", json);
            self.$nextTick(function() {
              self.internalChange = false;
            });
          } catch (e) {
            self.$emit("has-error", e);
          }
        },
      };

      this.editor = new JsonEditor(
        this.$el.querySelector(".vue-json-editor"),
        options,
        this.json
      );
    }
  }
};
</script>

<style lang="css">
 .vue-json-editor .jsoneditor {
   border: 1px solid #ced4da;
   border-radius: .25rem;
   padding: .375rem .75rem;
 }
</style>
