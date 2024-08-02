"""
@generated by mypy-protobuf.  Do not edit manually!
isort:skip_file
 **** IMPORTANT NOTE ****

 The proto of BT data has to match exactly the g3 proto, including tag
 number.
"""

import builtins
import collections.abc
import google.protobuf.descriptor
import google.protobuf.internal.containers
import google.protobuf.message
import typing

DESCRIPTOR: google.protobuf.descriptor.FileDescriptor

@typing.final
class StatVarGroups(google.protobuf.message.Message):
    DESCRIPTOR: google.protobuf.descriptor.Descriptor

    @typing.final
    class StatVarGroupsEntry(google.protobuf.message.Message):
        DESCRIPTOR: google.protobuf.descriptor.Descriptor

        KEY_FIELD_NUMBER: builtins.int
        VALUE_FIELD_NUMBER: builtins.int
        key: builtins.str
        @property
        def value(self) -> global___StatVarGroupNode: ...
        def __init__(
            self,
            *,
            key: builtins.str = ...,
            value: global___StatVarGroupNode | None = ...,
        ) -> None: ...
        def HasField(self, field_name: typing.Literal["value", b"value"]) -> builtins.bool: ...
        def ClearField(self, field_name: typing.Literal["key", b"key", "value", b"value"]) -> None: ...

    STAT_VAR_GROUPS_FIELD_NUMBER: builtins.int
    @property
    def stat_var_groups(self) -> google.protobuf.internal.containers.MessageMap[builtins.str, global___StatVarGroupNode]:
        """Key is StatVarGroup ID."""

    def __init__(
        self,
        *,
        stat_var_groups: collections.abc.Mapping[builtins.str, global___StatVarGroupNode] | None = ...,
    ) -> None: ...
    def ClearField(self, field_name: typing.Literal["stat_var_groups", b"stat_var_groups"]) -> None: ...

global___StatVarGroups = StatVarGroups

@typing.final
class StatVarGroupNode(google.protobuf.message.Message):
    DESCRIPTOR: google.protobuf.descriptor.Descriptor

    @typing.final
    class ChildSVG(google.protobuf.message.Message):
        DESCRIPTOR: google.protobuf.descriptor.Descriptor

        ID_FIELD_NUMBER: builtins.int
        SPECIALIZED_ENTITY_FIELD_NUMBER: builtins.int
        DISPLAY_NAME_FIELD_NUMBER: builtins.int
        DESCENDENT_STAT_VAR_COUNT_FIELD_NUMBER: builtins.int
        id: builtins.str
        """StatVarGroup ID."""
        specialized_entity: builtins.str
        """The specialized entity of the child StatVarGroup relative to the parent.
        This can be used for naming when the child appears in the hierarchy.
        """
        display_name: builtins.str
        """==== Below are fields not in original cache.
        Name suitable for display in tree.
        """
        descendent_stat_var_count: builtins.int
        """Number of unique descendent stat-vars."""
        def __init__(
            self,
            *,
            id: builtins.str = ...,
            specialized_entity: builtins.str = ...,
            display_name: builtins.str = ...,
            descendent_stat_var_count: builtins.int = ...,
        ) -> None: ...
        def ClearField(self, field_name: typing.Literal["descendent_stat_var_count", b"descendent_stat_var_count", "display_name", b"display_name", "id", b"id", "specialized_entity", b"specialized_entity"]) -> None: ...

    @typing.final
    class ChildSV(google.protobuf.message.Message):
        DESCRIPTOR: google.protobuf.descriptor.Descriptor

        ID_FIELD_NUMBER: builtins.int
        SEARCH_NAME_FIELD_NUMBER: builtins.int
        SEARCH_NAMES_FIELD_NUMBER: builtins.int
        DISPLAY_NAME_FIELD_NUMBER: builtins.int
        DEFINITION_FIELD_NUMBER: builtins.int
        HAS_DATA_FIELD_NUMBER: builtins.int
        id: builtins.str
        """StatVar ID."""
        search_name: builtins.str
        """Name suitable for search."""
        display_name: builtins.str
        """Name suitable for display in tree."""
        definition: builtins.str
        """Serialized string containing StatVar definition.

        The format is P=V delimited by commas. The required properties are
        abbreviated (populationType is 'pt', statType is 'st', etc).  For
        example, "median income of women" is:

           "st=medianValue,pt=Person,mp=income,gender=Female"

        When statType is "measuredValue" (default), it is skipped.
        """
        has_data: builtins.bool
        """==== Below are fields not in original cache.
        ==== and thus we start with a large tag number.

        Whether there is a data for this stat var
        """
        @property
        def search_names(self) -> google.protobuf.internal.containers.RepeatedScalarFieldContainer[builtins.str]:
            """Names suitable for search."""

        def __init__(
            self,
            *,
            id: builtins.str = ...,
            search_name: builtins.str = ...,
            search_names: collections.abc.Iterable[builtins.str] | None = ...,
            display_name: builtins.str = ...,
            definition: builtins.str = ...,
            has_data: builtins.bool = ...,
        ) -> None: ...
        def ClearField(self, field_name: typing.Literal["definition", b"definition", "display_name", b"display_name", "has_data", b"has_data", "id", b"id", "search_name", b"search_name", "search_names", b"search_names"]) -> None: ...

    ABSOLUTE_NAME_FIELD_NUMBER: builtins.int
    CHILD_STAT_VARS_FIELD_NUMBER: builtins.int
    CHILD_STAT_VAR_GROUPS_FIELD_NUMBER: builtins.int
    DESCENDENT_STAT_VAR_COUNT_FIELD_NUMBER: builtins.int
    PARENT_STAT_VAR_GROUPS_FIELD_NUMBER: builtins.int
    absolute_name: builtins.str
    """Absolute name of StatVarGroup. Typically used only for root nodes."""
    descendent_stat_var_count: builtins.int
    """Number of unique descendent stat-vars."""
    @property
    def child_stat_vars(self) -> google.protobuf.internal.containers.RepeatedCompositeFieldContainer[global___StatVarGroupNode.ChildSV]:
        """List of children StatVar IDs directly attached to this group. If there are
        auto-generated and curated IDs for a StatVar, we'll prefer the curated.
        """

    @property
    def child_stat_var_groups(self) -> google.protobuf.internal.containers.RepeatedCompositeFieldContainer[global___StatVarGroupNode.ChildSVG]:
        """List of children StatVarGroups that are immediate specializations."""

    @property
    def parent_stat_var_groups(self) -> google.protobuf.internal.containers.RepeatedScalarFieldContainer[builtins.str]:
        """==== Below are fields not in original cache.
        ==== and thus we start with a large tag number.

        List of parent StatVarGroup IDs.
        """

    def __init__(
        self,
        *,
        absolute_name: builtins.str = ...,
        child_stat_vars: collections.abc.Iterable[global___StatVarGroupNode.ChildSV] | None = ...,
        child_stat_var_groups: collections.abc.Iterable[global___StatVarGroupNode.ChildSVG] | None = ...,
        descendent_stat_var_count: builtins.int = ...,
        parent_stat_var_groups: collections.abc.Iterable[builtins.str] | None = ...,
    ) -> None: ...
    def ClearField(self, field_name: typing.Literal["absolute_name", b"absolute_name", "child_stat_var_groups", b"child_stat_var_groups", "child_stat_vars", b"child_stat_vars", "descendent_stat_var_count", b"descendent_stat_var_count", "parent_stat_var_groups", b"parent_stat_var_groups"]) -> None: ...

global___StatVarGroupNode = StatVarGroupNode