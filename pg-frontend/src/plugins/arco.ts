import { App } from "vue";
import Alert from "@arco-design/web-vue/es/alert";
import "@arco-design/web-vue/es/alert/style/css.js";
import Avatar from "@arco-design/web-vue/es/avatar";
import "@arco-design/web-vue/es/avatar/style/css.js";
import Button from "@arco-design/web-vue/es/button";
import "@arco-design/web-vue/es/button/style/css.js";
import Card from "@arco-design/web-vue/es/card";
import "@arco-design/web-vue/es/card/style/css.js";
import Descriptions from "@arco-design/web-vue/es/descriptions";
import "@arco-design/web-vue/es/descriptions/style/css.js";
import Divider from "@arco-design/web-vue/es/divider";
import "@arco-design/web-vue/es/divider/style/css.js";
import Dropdown from "@arco-design/web-vue/es/dropdown";
import "@arco-design/web-vue/es/dropdown/style/css.js";
import Form from "@arco-design/web-vue/es/form";
import "@arco-design/web-vue/es/form/style/css.js";
import Grid from "@arco-design/web-vue/es/grid";
import "@arco-design/web-vue/es/grid/style/css.js";
import Input from "@arco-design/web-vue/es/input";
import "@arco-design/web-vue/es/input/style/css.js";
import InputNumber from "@arco-design/web-vue/es/input-number";
import "@arco-design/web-vue/es/input-number/style/css.js";
import InputTag from "@arco-design/web-vue/es/input-tag";
import "@arco-design/web-vue/es/input-tag/style/css.js";
import Layout from "@arco-design/web-vue/es/layout";
import "@arco-design/web-vue/es/layout/style/css.js";
import Menu from "@arco-design/web-vue/es/menu";
import "@arco-design/web-vue/es/menu/style/css.js";
import Modal from "@arco-design/web-vue/es/modal";
import "@arco-design/web-vue/es/modal/style/css.js";
import Progress from "@arco-design/web-vue/es/progress";
import "@arco-design/web-vue/es/progress/style/css.js";
import Result from "@arco-design/web-vue/es/result";
import "@arco-design/web-vue/es/result/style/css.js";
import Select from "@arco-design/web-vue/es/select";
import "@arco-design/web-vue/es/select/style/css.js";
import Skeleton from "@arco-design/web-vue/es/skeleton";
import "@arco-design/web-vue/es/skeleton/style/css.js";
import Space from "@arco-design/web-vue/es/space";
import "@arco-design/web-vue/es/space/style/css.js";
import Spin from "@arco-design/web-vue/es/spin";
import "@arco-design/web-vue/es/spin/style/css.js";
import Table from "@arco-design/web-vue/es/table";
import "@arco-design/web-vue/es/table/style/css.js";
import Tabs from "@arco-design/web-vue/es/tabs";
import "@arco-design/web-vue/es/tabs/style/css.js";
import Tag from "@arco-design/web-vue/es/tag";
import "@arco-design/web-vue/es/tag/style/css.js";
import Textarea from "@arco-design/web-vue/es/textarea";
import "@arco-design/web-vue/es/textarea/style/css.js";
import "@arco-design/web-vue/es/message/style/css.js";

// Parent plugins also register their subcomponents (FormItem, Row, TabPane, etc.).
export function registerArco(app: App) {
  [
    Alert,
    Avatar,
    Button,
    Card,
    Descriptions,
    Divider,
    Dropdown,
    Form,
    Grid,
    Input,
    InputNumber,
    InputTag,
    Layout,
    Menu,
    Modal,
    Progress,
    Result,
    Select,
    Skeleton,
    Space,
    Spin,
    Table,
    Tabs,
    Tag,
    Textarea,
  ].forEach((component) => app.use(component));
}
