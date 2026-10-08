package chaos.s29.x13half.p1;

import chaos.s29.x13half.p1.meta.C29LeafMeta;
import com.google.common.collect.ImmutableList;
import com.rosetta.model.lib.RosettaModelObject;
import com.rosetta.model.lib.RosettaModelObjectBuilder;
import com.rosetta.model.lib.annotations.Accessor;
import com.rosetta.model.lib.annotations.AccessorType;
import com.rosetta.model.lib.annotations.Multi;
import com.rosetta.model.lib.annotations.Required;
import com.rosetta.model.lib.annotations.RosettaAttribute;
import com.rosetta.model.lib.annotations.RosettaDataType;
import com.rosetta.model.lib.annotations.RuneAttribute;
import com.rosetta.model.lib.annotations.RuneDataType;
import com.rosetta.model.lib.meta.RosettaMetaData;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.process.BuilderMerger;
import com.rosetta.model.lib.process.BuilderProcessor;
import com.rosetta.model.lib.process.Processor;
import com.rosetta.util.ListEquals;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static java.util.Optional.ofNullable;

/**
 * Helper - relocated by the import axis.
 * @version 1.0.0
 */
@RosettaDataType(value="C29Leaf", builder=C29Leaf.C29LeafBuilderImpl.class, version="1.0.0")
@RuneDataType(value="C29Leaf", model="chaos", builder=C29Leaf.C29LeafBuilderImpl.class, version="1.0.0")
public interface C29Leaf extends RosettaModelObject {

	C29LeafMeta metaData = new C29LeafMeta();

	/*********************** Getter Methods  ***********************/
	BigDecimal getV();
	List<String> getTags();

	/*********************** Build Methods  ***********************/
	C29Leaf build();
	
	C29Leaf.C29LeafBuilder toBuilder();
	
	static C29Leaf.C29LeafBuilder builder() {
		return new C29Leaf.C29LeafBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends C29Leaf> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends C29Leaf> getType() {
		return C29Leaf.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processor.processBasic(path.newSubPath("v"), BigDecimal.class, getV(), this);
		processor.processBasic(path.newSubPath("tags"), String.class, getTags(), this);
	}
	

	/*********************** Builder Interface  ***********************/
	interface C29LeafBuilder extends C29Leaf, RosettaModelObjectBuilder {
		C29Leaf.C29LeafBuilder setV(BigDecimal v);
		C29Leaf.C29LeafBuilder addTags(String tags);
		C29Leaf.C29LeafBuilder addTags(String tags, int idx);
		C29Leaf.C29LeafBuilder addTags(List<String> tags);
		C29Leaf.C29LeafBuilder setTags(List<String> tags);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processor.processBasic(path.newSubPath("v"), BigDecimal.class, getV(), this);
			processor.processBasic(path.newSubPath("tags"), String.class, getTags(), this);
		}
		

		C29Leaf.C29LeafBuilder prune();
	}

	/*********************** Immutable Implementation of C29Leaf  ***********************/
	class C29LeafImpl implements C29Leaf {
		private final BigDecimal v;
		private final List<String> tags;
		
		protected C29LeafImpl(C29Leaf.C29LeafBuilder builder) {
			this.v = builder.getV();
			this.tags = ofNullable(builder.getTags()).filter(_l->!_l.isEmpty()).map(ImmutableList::copyOf).orElse(null);
		}
		
		@Override
		@RosettaAttribute("v")
		@Accessor(AccessorType.GETTER)
		@Required
		@RuneAttribute("v")
		public BigDecimal getV() {
			return v;
		}
		
		@Override
		@RosettaAttribute("tags")
		@Accessor(AccessorType.GETTER)
		@Multi
		@RuneAttribute("tags")
		public List<String> getTags() {
			return tags;
		}
		
		@Override
		public C29Leaf build() {
			return this;
		}
		
		@Override
		public C29Leaf.C29LeafBuilder toBuilder() {
			C29Leaf.C29LeafBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(C29Leaf.C29LeafBuilder builder) {
			ofNullable(getV()).ifPresent(builder::setV);
			ofNullable(getTags()).ifPresent(builder::setTags);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C29Leaf _that = getType().cast(o);
		
			if (!Objects.equals(v, _that.getV())) return false;
			if (!ListEquals.listEquals(tags, _that.getTags())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (v != null ? v.hashCode() : 0);
			_result = 31 * _result + (tags != null ? tags.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "C29Leaf {" +
				"v=" + this.v + ", " +
				"tags=" + this.tags +
			'}';
		}
	}

	/*********************** Builder Implementation of C29Leaf  ***********************/
	class C29LeafBuilderImpl implements C29Leaf.C29LeafBuilder {
	
		protected BigDecimal v;
		protected List<String> tags = new ArrayList<>();
		
		@Override
		@RosettaAttribute("v")
		@Accessor(AccessorType.GETTER)
		@Required
		@RuneAttribute("v")
		public BigDecimal getV() {
			return v;
		}
		
		@Override
		@RosettaAttribute("tags")
		@Accessor(AccessorType.GETTER)
		@Multi
		@RuneAttribute("tags")
		public List<String> getTags() {
			return tags;
		}
		
		@RosettaAttribute("v")
		@Accessor(AccessorType.SETTER)
		@Required
		@RuneAttribute("v")
		@Override
		public C29Leaf.C29LeafBuilder setV(BigDecimal _v) {
			this.v = _v == null ? null : _v;
			return this;
		}
		
		@RosettaAttribute("tags")
		@Accessor(AccessorType.ADDER)
		@Multi
		@RuneAttribute("tags")
		@Override
		public C29Leaf.C29LeafBuilder addTags(String _tags) {
			if (_tags != null) {
				this.tags.add(_tags);
			}
			return this;
		}
		
		@Override
		public C29Leaf.C29LeafBuilder addTags(String _tags, int idx) {
			getIndex(this.tags, idx, () -> _tags);
			return this;
		}
		
		@Override
		public C29Leaf.C29LeafBuilder addTags(List<String> tagss) {
			if (tagss != null) {
				for (final String toAdd : tagss) {
					this.tags.add(toAdd);
				}
			}
			return this;
		}
		
		@RosettaAttribute("tags")
		@Accessor(AccessorType.SETTER)
		@Multi
		@RuneAttribute("tags")
		@Override
		public C29Leaf.C29LeafBuilder setTags(List<String> tagss) {
			if (tagss == null) {
				this.tags = new ArrayList<>();
			} else {
				this.tags = tagss.stream()
					.collect(Collectors.toCollection(()->new ArrayList<>()));
			}
			return this;
		}
		
		@Override
		public C29Leaf build() {
			return new C29Leaf.C29LeafImpl(this);
		}
		
		@Override
		public C29Leaf.C29LeafBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public C29Leaf.C29LeafBuilder prune() {
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (getV()!=null) return true;
			if (getTags()!=null && !getTags().isEmpty()) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public C29Leaf.C29LeafBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			C29Leaf.C29LeafBuilder o = (C29Leaf.C29LeafBuilder) other;
			
			
			merger.mergeBasic(getV(), o.getV(), this::setV);
			merger.mergeBasic(getTags(), o.getTags(), (Consumer<String>) this::addTags);
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C29Leaf _that = getType().cast(o);
		
			if (!Objects.equals(v, _that.getV())) return false;
			if (!ListEquals.listEquals(tags, _that.getTags())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (v != null ? v.hashCode() : 0);
			_result = 31 * _result + (tags != null ? tags.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "C29LeafBuilder {" +
				"v=" + this.v + ", " +
				"tags=" + this.tags +
			'}';
		}
	}
}
